"""Integracao real em banco temporario criado pelo GitHub Actions.
Nunca executar apontando para o banco pessoal ou para dados de producao.
"""
import base64
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
AUDIT = ROOT / ".audit"
AUDIT.mkdir(exist_ok=True)
BASE = "http://127.0.0.1:8089"
BANK_URL = "https://1drv.ms/u/c/5c4d97af97bbeda3/IQDnxKul4GvCSYRzyiHY_HVHAQOU4_I3essWXrwXnWGhdOM?e=hWntF9"
RESULT = {"commit_codigo": os.environ.get("AUDIT_SOURCE_COMMIT"), "verificacoes": [], "onedrive_verificado": False}
PROCESS = None
LOG = None


def check(value, name):
    if not value:
        raise AssertionError(name)
    RESULT["verificacoes"].append(name)
    print("OK:", name, flush=True)


def sql(text):
    command = [
        "docker", "exec", "-i", "-e", "MYSQL_PWD=" + os.environ["MYSQL_PWD"],
        os.environ["MYSQL_CONTAINER"], "mysql", "-uroot",
        "--default-character-set=utf8mb4", "--batch", "--skip-column-names"
    ]
    r = subprocess.run(command, input=text, text=True, encoding="utf-8", capture_output=True, timeout=60)
    if r.returncode:
        raise RuntimeError("MySQL: " + r.stderr[-3000:])
    return r.stdout.strip()


def query(text):
    return sql("USE conexagro;\n" + text)


def opener():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))


ADMIN = opener()
USER = opener()
ANON = opener()


def request(client, method, path, body=None, status=200, headers=None, raw=False):
    h = {"X-ConexAgro-Request": "1", "Content-Type": "application/json"}
    h.update(headers or {})
    data = body if isinstance(body, bytes) else (json.dumps(body or {}).encode() if method != "GET" else None)
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=h)
    try:
        response = client.open(req, timeout=20)
    except urllib.error.HTTPError as error:
        response = error
    payload = response.read()
    check(response.code == status, method + " " + path + " -> " + str(status) + ("" if response.code == status else ": " + payload.decode(errors="replace")))
    if raw:
        return payload
    return json.loads(payload) if payload else {}


def start():
    global PROCESS, LOG
    LOG = (AUDIT / "aplicacao.log").open("ab")
    PROCESS = subprocess.Popen(["java", "-cp", str(AUDIT / "classes") + os.pathsep + str(AUDIT / "mysql.jar"), "Principal"], stdout=LOG, stderr=subprocess.STDOUT)
    for _ in range(120):
        if PROCESS.poll() is not None:
            raise RuntimeError((AUDIT / "aplicacao.log").read_text(errors="replace"))
        try:
            with urllib.request.urlopen(BASE + "/api/status", timeout=1) as r:
                if r.status == 200:
                    return
        except (OSError, urllib.error.URLError):
            time.sleep(0.25)
    raise RuntimeError("O servidor nao iniciou no prazo.")


def stop():
    global PROCESS, LOG
    if PROCESS:
        PROCESS.terminate()
        try:
            PROCESS.wait(timeout=10)
        except subprocess.TimeoutExpired:
            PROCESS.kill()
            PROCESS.wait(timeout=5)
        PROCESS = None
    if LOG:
        LOG.close()
        LOG = None


def banco():
    fixture = (ROOT / "tests/fixtures/ConexAgro_POO_Banco_Completo.sql").read_bytes()
    RESULT["sha256_sql_fixture"] = hashlib.sha256(fixture).hexdigest()
    selected = fixture
    encoded = base64.urlsafe_b64encode(BANK_URL.encode()).decode().rstrip("=")
    urls = [
        "https://api.onedrive.com/v1.0/shares/u!" + encoded + "/root/content",
        BANK_URL + "&download=1",
    ]
    errors = []
    for url in urls:
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=12) as response:
                data = response.read(2 * 1024 * 1024 + 1)
            decoded = data.decode("utf-8-sig")
            if len(data) > 2 * 1024 * 1024 or not re.search(r"CREATE\s+TABLE\s+(IF\s+NOT\s+EXISTS\s+)?usuario", decoded, re.I):
                raise ValueError("A resposta nao e o script SQL esperado.")
            selected = data
            RESULT["onedrive_verificado"] = True
            RESULT["sha256_sql_onedrive"] = hashlib.sha256(data).hexdigest()
            RESULT["onedrive_igual_fixture"] = decoded.replace("\r\n", "\n").strip() == fixture.decode("utf-8-sig").replace("\r\n", "\n").strip()
            print("SQL obtido pelo link do OneDrive.", flush=True)
            break
        except Exception as error:
            errors.append(type(error).__name__ + ": " + str(error)[:250])
    if not RESULT["onedrive_verificado"]:
        RESULT["limitacao_onedrive"] = errors
        print("AVISO: OneDrive indisponivel; usando a versao 1.1 do SQL entregue nesta conversa.", flush=True)
    RESULT["sha256_sql_testado"] = hashlib.sha256(selected).hexdigest()
    sql(selected.decode("utf-8-sig"))
    expected = "SELECT (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='conexagro'),(SELECT COUNT(*) FROM peca),(SELECT COUNT(*) FROM maquina),(SELECT COUNT(*) FROM categoria),(SELECT COUNT(*) FROM imagem),(SELECT COUNT(*) FROM usuario);"
    counts = query(expected)
    check(counts == "15\t11\t4\t7\t10\t0", "Instalacao limpa: 15 tabelas, 11 pecas, 4 maquinas, 7 categorias, 10 imagens e nenhuma conta fixa")
    sql(selected.decode("utf-8-sig"))
    check(query(expected) == counts, "Reexecucao do SQL sem duplicar os registros")
    RESULT["mysql"] = sql("SELECT VERSION();")
    links = query("SELECT url_imagem FROM imagem;").splitlines()
    check(all((ROOT / "dados/imagens" / Path(u).name).is_file() for u in links), "As dez imagens do SQL existem no projeto")
    (ROOT / "conexao.properties").write_text(
        "db.url=jdbc:mysql://127.0.0.1:3307/conexagro?sslMode=DISABLED&allowPublicKeyRetrieval=true&characterEncoding=UTF-8\n"
        "db.user=root\ndb.password=" + os.environ["MYSQL_PWD"] + "\nweb.port=8089\n", encoding="utf-8")


def tests():
    banco()
    start()
    check(request(ANON, "GET", "/api/status")["configuracaoPendente"], "Primeiro acesso exige configurar o administrador")
    for _ in range(20):
        match = re.search(r"/#configurar=([A-Za-z0-9_-]{43})", (AUDIT / "aplicacao.log").read_text(errors="replace"))
        if match:
            break
        time.sleep(0.1)
    check(match is not None, "Principal informa o endereco temporario de configuracao")
    admin_data = {"nome": "Administrador CI", "email": "admin@ci.example.test", "senha": "Senha-CI-2026!"}
    data = request(ADMIN, "POST", "/api/configuracao", admin_data, headers={"X-ConexAgro-Setup": match[1]})
    admin_id = data["usuario"]["id"]
    check(data["usuario"]["perfil"] == "administrador", "Primeiro administrador criado e autenticado")
    request(ANON, "POST", "/api/configuracao", admin_data, status=403, headers={"X-ConexAgro-Setup": match[1]})
    request(ANON, "GET", "/api/pecas", status=401)
    check(not request(ADMIN, "GET", "/api/status")["configuracaoPendente"], "Configuracao inicial encerrada")
    page = request(ADMIN, "GET", "/api/pecas")
    check(page["total"] == 10, "Catalogo publica as dez pecas completas e oculta o rascunho")
    first = request(ADMIN, "GET", "/api/pecas/" + str(page["itens"][0]["id"]))
    check(bool(first["codigos"] and first["imagens"] and first["especificacoes"] and first["compatibilidades"]), "Ficha tecnica possui todos os detalhes")
    photo = request(ANON, "GET", first["imagens"][0]["url"], raw=True)
    check(photo.startswith(b"\x89PNG"), "Ilustracao do catalogo carregada pelo servidor Java")
    apoio = request(ADMIN, "GET", "/api/apoio")
    marca = apoio["marcas"][0]["id"]
    origem = apoio["origens"][0]["id"]
    maquina = apoio["maquinas"][0]["id"]

    # CRUD completo das duas entidades relacionadas.
    categoria = request(ADMIN, "POST", "/api/categorias", {"nome": "CI Categoria acao"})
    cid = categoria["id"]
    request(ADMIN, "POST", "/api/categorias", {"nome": "CI Categoria acao"}, status=409)
    request(ADMIN, "PUT", "/api/categorias/" + str(cid), {"nome": "CI Categoria atualizada"})
    check(any(x["id"] == cid and x["nome"] == "CI Categoria atualizada" for x in request(ADMIN, "GET", "/api/apoio")["categorias"]), "Categoria alterada persiste na consulta")
    peca = request(ADMIN, "POST", "/api/pecas", {"nome": "CI Peca rascunho", "categoria": cid})
    pid = peca["id"]
    check(peca["estado"] == "rascunho", "Rascunho aceita cadastro incompleto")
    check(query("SELECT COUNT(*) FROM item_catalogo i JOIN peca p ON p.id_peca=i.id_item WHERE i.id_item=" + str(pid) + ";") == "1", "Heranca: item_catalogo e peca compartilham o ID")
    changed = {"nome": "CI Peca alterada", "categoria": cid, "marca": marca, "descricao": "Descricao alterada para verificacao"}
    request(ADMIN, "PUT", "/api/pecas/" + str(pid), changed)
    persisted = request(ADMIN, "GET", "/api/pecas/" + str(pid))
    check(persisted["nome"] == changed["nome"] and persisted["categoria"]["id"] == cid, "Atualizacao de peca preserva o relacionamento com categoria")
    request(ADMIN, "DELETE", "/api/categorias/" + str(cid), status=409)
    request(ADMIN, "POST", "/api/pecas/" + str(pid) + "/publicar", status=400)
    check(request(ADMIN, "GET", "/api/pecas/" + str(pid))["estado"] == "rascunho", "Publicacao incompleta nao altera o estado")
    request(ADMIN, "DELETE", "/api/pecas/" + str(pid))
    request(ADMIN, "GET", "/api/pecas/" + str(pid), status=404)
    check(query("SELECT COUNT(*) FROM item_catalogo WHERE id_item=" + str(pid) + ";") == "0", "Exclusao do rascunho remove o item base")
    request(ADMIN, "DELETE", "/api/categorias/" + str(cid))
    check(not any(x["id"] == cid for x in request(ADMIN, "GET", "/api/apoio")["categorias"]), "Categoria livre pode ser excluida")

    # CRUD dos cadastros auxiliares.
    for route, body, updated in [
        ("marcas", {"nome": "CI Marca"}, {"nome": "CI Marca atualizada"}),
        ("origens", {"nome": "CI Origem", "tipo": "fornecedor"}, {"nome": "CI Origem atualizada", "tipo": "fabricante"}),
    ]:
        item = request(ADMIN, "POST", "/api/" + route, body)
        request(ADMIN, "PUT", "/api/" + route + "/" + str(item["id"]), updated)
        check(any(x["id"] == item["id"] and x["nome"] == updated["nome"] for x in request(ADMIN, "GET", "/api/apoio")[route]), "Consulta e alteracao de " + route)
        request(ADMIN, "DELETE", "/api/" + route + "/" + str(item["id"]))
    body = {"nome": "CI Maquina", "modelo": "CI-001", "tipoMaquina": "Trator", "marca": marca, "estado": "rascunho"}
    machine = request(ADMIN, "POST", "/api/maquinas", body)
    body["modelo"] = "CI-002"
    request(ADMIN, "PUT", "/api/maquinas/" + str(machine["id"]), body)
    check(any(x["id"] == machine["id"] and x["modelo"] == "CI-002" for x in request(ADMIN, "GET", "/api/apoio")["maquinas"]), "Maquina atualizada pode ser consultada")
    request(ADMIN, "DELETE", "/api/maquinas/" + str(machine["id"]))

    # Detalhes, publicacao e rollback.
    cid = request(ADMIN, "POST", "/api/categorias", {"nome": "CI Publicacao"})["id"]
    pid = request(ADMIN, "POST", "/api/pecas", {"nome": "CI Peca publicavel", "categoria": cid, "marca": marca, "descricao": "Peca ficticia para a integracao"})["id"]
    endpoint = "/api/pecas/" + str(pid)
    code = request(ADMIN, "POST", endpoint + "/codigos", {"marca": marca, "origem": origem, "codigo": "CI-CRUD-001", "tipo": "original"})
    request(ADMIN, "PUT", endpoint + "/codigos/" + str(code["id"]), {"marca": marca, "origem": origem, "codigo": "CI-CRUD-002", "tipo": "paralelo"})
    spec = request(ADMIN, "POST", endpoint + "/especificacoes", {"nome": "Altura", "valor": "12", "unidade": "mm"})
    request(ADMIN, "PUT", endpoint + "/especificacoes/" + str(spec["id"]), {"nome": "Altura", "valor": "15", "unidade": "mm"})
    request(ADMIN, "DELETE", endpoint + "/especificacoes/" + str(spec["id"]))
    spec = request(ADMIN, "POST", endpoint + "/especificacoes", {"nome": "Altura", "valor": "15", "unidade": "mm"})
    png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/l9sAAAAASUVORK5CYII=")
    image = request(ADMIN, "POST", endpoint + "/imagens/upload?descricao=CI", png, status=201, headers={"Content-Type": "image/png"})
    request(ADMIN, "PUT", endpoint + "/imagens/" + str(image["id"]), {"url": image["url"], "descricao": "Imagem atualizada CI"})
    check(request(ANON, "GET", image["url"], raw=True) == png, "Upload entrega os mesmos bytes da imagem")
    compat = {"maquina": maquina, "estado": "pendente"}
    request(ADMIN, "POST", endpoint + "/compatibilidades", compat)
    request(ADMIN, "DELETE", endpoint + "/compatibilidades/" + str(maquina))
    compat.update({"estado": "confirmada", "fonte": "Simulacao academica de integracao", "anoInicio": 2018, "anoFim": 2026})
    request(ADMIN, "POST", endpoint + "/compatibilidades", compat)
    request(ADMIN, "POST", endpoint + "/publicar")
    check(request(ADMIN, "GET", endpoint)["estado"] == "publicado", "Peca com ficha completa pode ser publicada")
    check(request(ADMIN, "GET", "/api/pecas?termo=CI-CRUD-002")["total"] == 1, "Pesquisa localiza o codigo alterado")
    check(request(ADMIN, "GET", "/api/pecas?maquina=" + str(maquina) + "&ano=2020&termo=CI-CRUD-002")["total"] == 1, "Filtros por maquina e ano encontram a compatibilidade")
    check(request(ADMIN, "GET", "/api/pecas?maquina=" + str(maquina) + "&ano=2010&termo=CI-CRUD-002")["total"] == 0, "Filtro exclui anos fora da compatibilidade")
    for detail, did in [("codigos", code["id"]), ("imagens", image["id"]), ("especificacoes", spec["id"]), ("compatibilidades", maquina)]:
        request(ADMIN, "DELETE", endpoint + "/" + detail + "/" + str(did), status=400)
    ficha = request(ADMIN, "GET", endpoint)
    check(all(len(ficha[key]) == 1 for key in ["codigos", "imagens", "especificacoes", "compatibilidades"]), "Rollback preserva os ultimos detalhes de uma peca publicada")
    request(ADMIN, "DELETE", endpoint, status=400)

    # Cadastro, autenticacao, permissoes e CRUD de usuarios.
    userbody = {"nome": "Usuario CI", "email": "usuario@ci.example.test", "senha": "Senha-Usuario-CI!", "ocupacao": "vendedor", "perfil": "administrador"}
    registered = request(ANON, "POST", "/api/registro", userbody, status=201)
    uid = registered["id"]
    check(registered["perfil"] == "usuario", "Registro publico nao aceita promocao a administrador")
    request(USER, "POST", "/api/login", userbody)
    check(request(USER, "GET", endpoint)["id"] == pid, "Usuario comum consulta ficha publicada")
    request(USER, "POST", "/api/categorias", {"nome": "Proibida"}, status=403)
    request(USER, "GET", "/api/usuarios", status=403)
    request(ADMIN, "POST", endpoint + "/inativar")
    request(USER, "GET", endpoint, status=404)
    check(request(USER, "GET", "/api/pecas?termo=CI-CRUD-002")["total"] == 0, "Peca inativada deixa o catalogo publico")
    request(ADMIN, "POST", endpoint + "/publicar")
    request(USER, "PUT", "/api/perfil", {"nome": "Usuario CI atualizado", "email": userbody["email"], "ocupacao": "mecanico", "perfil": "administrador"})
    profile = request(USER, "GET", "/api/perfil")
    check(profile["nome"] == "Usuario CI atualizado" and profile["perfil"] == "usuario", "Edicao pessoal atualiza os dados e preserva o perfil")
    request(USER, "POST", "/api/perfil/senha", {"atual": userbody["senha"], "nova": "Nova-Senha-Usuario-CI!"})
    request(USER, "GET", "/api/perfil", status=401)
    request(USER, "POST", "/api/login", {"email": userbody["email"], "senha": userbody["senha"]}, status=401)
    request(USER, "POST", "/api/login", {"email": userbody["email"], "senha": "Nova-Senha-Usuario-CI!"})
    request(ADMIN, "GET", "/api/usuarios/" + str(uid))
    change = {"nome": "Usuario editado por admin", "email": userbody["email"], "ocupacao": "agricultor", "perfil": "usuario", "ativo": False}
    request(ADMIN, "PUT", "/api/usuarios/" + str(uid), change)
    request(USER, "GET", "/api/perfil", status=401)
    request(ADMIN, "DELETE", "/api/usuarios/" + str(uid))
    request(ADMIN, "GET", "/api/usuarios/" + str(uid), status=404)
    request(ADMIN, "DELETE", "/api/usuarios/" + str(admin_id), status=409)
    check(query("SELECT COUNT(*) FROM usuario WHERE id_usuario=" + str(uid) + ";") == "0", "Exclusao de usuario persiste no banco")
    request(ADMIN, "POST", "/api/logout")
    request(ADMIN, "GET", "/api/perfil", status=401)

    stop()
    start()
    check(not request(ANON, "GET", "/api/status")["configuracaoPendente"], "Reiniciar a Principal preserva o administrador")
    request(ADMIN, "POST", "/api/login", admin_data)
    check(request(ADMIN, "GET", endpoint)["estado"] == "publicado", "Reiniciar o sistema preserva os dados cadastrados")
    check(query("SELECT COUNT(*) FROM item_catalogo i LEFT JOIN peca p ON p.id_peca=i.id_item LEFT JOIN maquina m ON m.id_maquina=i.id_item WHERE (i.tipo_item='peca' AND p.id_peca IS NULL) OR (i.tipo_item='maquina' AND m.id_maquina IS NULL);") == "0", "Nenhum item base ficou sem o subtipo correspondente")
    RESULT["status"] = "aprovado"


if __name__ == "__main__":
    try:
        tests()
    except Exception as error:
        RESULT["status"] = "reprovado"
        RESULT["erro"] = type(error).__name__ + ": " + str(error)
        raise
    finally:
        stop()
        RESULT["total_verificacoes"] = len(RESULT["verificacoes"])
        (AUDIT / "resultado.json").write_text(json.dumps(RESULT, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("RESULTADO:", RESULT["status"], "-", RESULT["total_verificacoes"], "verificacoes", flush=True)
