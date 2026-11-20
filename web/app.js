'use strict';
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));
const S = {
    user: null,
    page: 'catalogo',
    apoio: {marcas: [], categorias: [], origens: [], maquinas: []},
    query: {},
    adminQuery: {},
    pagina: 1,
    adminPagina: 1,
    apoioTab: 'marcas',
    edit: null,
    editTab: 'dados',
    editDetail: null,
    users: [],
    setup: null
};
const E = v => String(v ?? '').replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
}[c]));
const I = n => `<svg class="icon" aria-hidden="true"><use href="#i-${n}"/></svg>`;
const labels = {
    usuario: 'Usuário',
    administrador: 'Administrador',
    vendedor: 'Vendedor',
    agricultor: 'Agricultor',
    mecanico: 'Mecânico',
    rascunho: 'Rascunho',
    publicado: 'Publicado',
    inativo: 'Inativo',
    ativo: 'Ativo',
    pendente: 'Pendente',
    confirmada: 'Confirmada',
    original: 'Original',
    paralelo: 'Paralelo',
    fornecedor: 'Fornecedor',
    fabricante: 'Fabricante'
};
const L = v => labels[v] || v || 'Não informado';
const admin = () => S.user?.perfil === 'administrador';
const badge = v => `<span class="badge ${E(v)}">${E(L(v))}</span>`;
const options = (list, selected, blank = 'Selecione…') => `<option value="">${E(blank)}</option>` + list.map(o => `<option value="${o.id}" ${String(o.id) === String(selected ?? '') ? 'selected' : ''}>${E(o.nome || o.modelo)}</option>`).join('');
const enumOptions = (values, selected, blank) => `${blank ? `<option value="">${E(blank)}</option>` : ''}` + values.map(v => `<option value="${v}" ${v === selected ? 'selected' : ''}>${E(L(v))}</option>`).join('');
let fieldSequence = 0;
const field = (label, name, value = '', extra = '', type = 'text') => {
    const id = 'field-' + (++fieldSequence);
    return `<div class="field"><label for="${id}">${E(label)}</label><input id="${id}" name="${E(name)}" type="${type}" value="${E(value)}" ${extra}></div>`;
};
const selectField = (label, name, content, extra = '') => {
    const id = 'field-' + (++fieldSequence);
    return `<div class="field"><label for="${id}">${E(label)}</label><select id="${id}" name="${E(name)}" ${extra}>${content}</select></div>`;
};
const num = v => v === '' || v == null ? null : Number(v);
const formData = f => Object.fromEntries(new FormData(f).entries());
const modal = $('#modal');
const closeModal = () => {
    if (modal.open) modal.close();
};

function notify(message, error = false) {
    if (modal.open) {
        let n = $('.modal-notice', modal);
        if (!n) {
            n = document.createElement('div');
            n.className = 'notice modal-notice';
            n.setAttribute('role', 'status');
            $('.modal-head', modal)?.after(n);
        }
        n.className = 'notice modal-notice' + (error ? ' error' : '');
        n.textContent = message;
        setTimeout(() => n.remove(), 6500);
        return;
    }
    const t = document.createElement('div');
    t.className = 'toast' + (error ? ' error' : '');
    t.innerHTML = I(error ? 'info' : 'check') + `<span>${E(message)}</span>`;
    $('#toasts').append(t);
    setTimeout(() => t.remove(), 5500);
}

async function api(path, {method = 'GET', data, file, setup} = {}) {
    const headers = {};
    if (method !== 'GET') headers['X-ConexAgro-Request'] = '1';
    if (setup) headers['X-ConexAgro-Setup'] = setup;
    let body;
    if (file) {
        headers['Content-Type'] = file.type || 'application/octet-stream';
        body = file;
    } else if (method !== 'GET') {
        headers['Content-Type'] = 'application/json';
        body = JSON.stringify(data || {});
    }
    let response;
    try {
        response = await fetch(path, {method, headers, body, credentials: 'same-origin'});
    } catch (e) {
        throw new Error(location.protocol === 'file:' ? 'Execute a Principal no IntelliJ e abra o endereço exibido no console.' : 'Não foi possível comunicar com o sistema. Confira se a execução no IntelliJ continua aberta.');
    }
    const result = await response.json().catch(() => ({erro: 'Resposta inesperada do sistema.'}));
    if (!response.ok) {
        if (response.status === 401 && S.user) showAuth('login');
        const e = new Error(result.erro || 'Não foi possível concluir a operação.');
        e.status = response.status;
        throw e;
    }
    return result;
}

const queryUrl = (base, obj) => {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(obj)) if (v !== '' && v != null) p.set(k, v);
    return base + (p.size ? '?' + p : '');
};

function showAuth(mode = 'login') {
    S.user = null;
    closeModal();
    $('#app').hidden = true;
    $('#auth').hidden = false;
    $('#boot').hidden = true;
    $('#auth-error').hidden = true;
    let html = '';
    if (mode === 'setup') html = `<span class="eyebrow">BEM-VINDO À CONEXAGRO</span><h2>Seu catálogo começa aqui.</h2><p class="auth-sub">Crie a conta administradora para organizar as peças e os acessos da sua equipe.</p><form class="form-stack" data-form="setup">${field('Nome completo', 'nome', '', 'required maxlength="120" autocomplete="name"')}${field('E-mail', 'email', '', 'required autocomplete="email"', 'email')}${field('Senha', 'senha', '', 'required minlength="8" maxlength="1024" autocomplete="new-password"', 'password')}${field('Confirme a senha', 'confirmacao', '', 'required minlength="8" autocomplete="new-password"', 'password')}<button class="button wide" type="submit">Criar acesso e começar ${I('arrow')}</button></form>`;
    else if (mode === 'registro') html = `<span class="eyebrow">ENCONTRE A REFERÊNCIA CERTA</span><h2>Crie sua conta.</h2><p class="auth-sub">Tenha acesso às informações e aplicações das peças agrícolas.</p><form class="form-stack" data-form="registro">${field('Nome completo', 'nome', '', 'required maxlength="120" autocomplete="name"')}${field('E-mail', 'email', '', 'required autocomplete="email"', 'email')}${selectField('Sua ocupação', 'ocupacao', enumOptions(['vendedor', 'agricultor', 'mecanico'], 'vendedor'), 'required')}${field('Senha', 'senha', '', 'required minlength="8" maxlength="1024" placeholder="Mínimo de 8 caracteres" autocomplete="new-password"', 'password')}${field('Confirme a senha', 'confirmacao', '', 'required minlength="8" autocomplete="new-password"', 'password')}<button class="button wide" type="submit">Criar conta ${I('arrow')}</button></form><p class="auth-switch">Já possui uma conta? <button class="text-button" data-action="auth-login">Entrar</button></p>`;
    else html = `<span class="eyebrow">INFORMAÇÃO PARA O CAMPO</span><h2>Bom ter você por aqui.</h2><p class="auth-sub">Entre para consultar peças, códigos e compatibilidades.</p><form class="form-stack" data-form="login">${field('E-mail', 'email', '', 'required placeholder="seu@email.com" autocomplete="username"', 'email')}${field('Senha', 'senha', '', 'required placeholder="Sua senha" autocomplete="current-password" maxlength="1024"', 'password')}<button class="button wide" type="submit">Entrar no catálogo ${I('arrow')}</button></form><p class="auth-switch">Ainda não possui uma conta? <button class="text-button" data-action="auth-registro">Criar conta</button></p>`;
    $('#auth-content').innerHTML = html;
}

async function enterApp(user) {
    S.user = user;
    $('#auth-content').innerHTML = '';
    $('#boot').hidden = true;
    $('#auth').hidden = true;
    $('#app').hidden = false;
    $('#user-name').textContent = user.nome;
    $('#user-role').textContent = L(user.perfil);
    $('#avatar').textContent = user.nome.split(/\s+/).filter(Boolean).slice(0, 2).map(v => v[0]).join('').toUpperCase();
    $$('.admin-only').forEach(el => el.hidden = !admin());
    await refreshApoio();
    await navigate('catalogo');
}

async function refreshApoio() {
    S.apoio = await api('/api/apoio');
}

async function navigate(page) {
    if (!S.user) return;
    if (['pecas', 'usuarios', 'apoio'].includes(page) && !admin()) page = 'catalogo';
    S.page = page;
    $('#sidebar').classList.remove('open');
    $$('[data-page]').forEach(b => b.classList.toggle('active', b.dataset.page === page));
    $('#breadcrumb').textContent = {
        catalogo: 'Catálogo',
        pecas: 'Peças',
        usuarios: 'Usuários',
        apoio: 'Cadastros de apoio',
        perfil: 'Meu perfil'
    }[page];
    const c = $('#content');
    c.innerHTML = '<div class="loading-line">Carregando informações…</div>';
    try {
        if (page === 'catalogo' || page === 'pecas') await renderPecas(page === 'pecas'); else if (page === 'usuarios') await renderUsers(); else if (page === 'apoio') {
            await refreshApoio();
            renderApoio();
        } else renderPerfil();
    } catch (e) {
        if (S.user) c.innerHTML = empty('Não foi possível carregar', e.message, 'info', 'retry', 'Tentar novamente');
    }
}

function empty(title, text, icon = 'search', action = '', label = '') {
    return `<div class="empty panel"><div class="empty-icon">${I(icon)}</div><h3>${E(title)}</h3><p>${E(text)}</p>${action ? `<button class="button secondary" data-action="${action}">${E(label)}</button>` : ''}</div>`;
}

function image(url, cls = '') {
    const safe = safeImage(url);
    return safe ? `<img class="${cls}" src="${E(safe)}" alt="" loading="lazy" data-photo><span class="image-fallback" hidden>${I('part')}</span>` : I('part');
}

function safeImage(url) {
    if (!url) return null;
    try {
        const u = new URL(url, location.origin);
        return ['http:', 'https:'].includes(u.protocol) && !u.username ? u.href : null;
    } catch {
        return null;
    }
}

function pageHead(title, text, button = '') {
    return `<div class="page-head"><div><span class="eyebrow">ÁREA ADMINISTRATIVA</span><h1>${E(title)}</h1><p>${E(text)}</p></div>${button}</div>`;
}

async function renderPecas(administracao = false) {
    const f = administracao ? S.adminQuery : S.query;
    $('#content').innerHTML = (administracao ? pageHead('Peças', 'Organize o catálogo. Comece com um rascunho e publique os dados conferidos.', `<button class="button" data-action="new-piece">${I('plus')} Nova peça</button>`) : `<div class="catalog-hero"><div><span class="eyebrow">PEÇAS E COMPATIBILIDADES</span><h1>Encontre a peça certa.</h1><p>Códigos, especificações e aplicações reunidos para facilitar sua consulta.</p></div><div class="hero-seal">${I('shield')}<span><strong>Confira a aplicação.</strong>Uma boa referência faz diferença.</span></div></div>`) + `
 <form class="panel search-panel" data-form="${administracao ? 'admin-search' : 'catalog-search'}"><div class="search-row"><div class="search-input">${I('search')}<input name="termo" value="${E(f.termo)}" placeholder="Busque por código, nome ou descrição da peça" aria-label="Código, nome ou descrição"></div><button class="button" type="submit">${I('search')}<span>Pesquisar</span></button></div><div class="filter-row">${selectField('Marca / fabricante', 'marca', options(S.apoio.marcas, f.marca, 'Todas as marcas'))}${selectField('Categoria', 'categoria', options(S.apoio.categorias, f.categoria, 'Todas as categorias'))}${administracao ? selectField('Estado do cadastro', 'estado', enumOptions(['rascunho', 'publicado', 'inativo'], f.estado, 'Todos os estados')) : selectField('Máquina / modelo', 'maquina', options(S.apoio.maquinas.map(m => ({
        ...m,
        nome: (m.marca?.nome ? m.marca.nome + ' · ' : '') + m.modelo
    })), f.maquina, 'Todas as máquinas'))}${administracao ? '<div></div>' : field('Ano de aplicação', 'ano', f.ano || '', 'min="1900" max="2200" placeholder="Ex.: 2020"', 'number')}<button class="text-button filter-clear" type="button" data-action="clear-search">Limpar filtros</button></div></form><div id="results-head" class="results-head"></div><div id="results"></div><div id="pagination"></div>`;
    await loadPecas(administracao);
}

async function loadPecas(administracao) {
    const f = administracao ? S.adminQuery : S.query, pagina = administracao ? S.adminPagina : S.pagina;
    const data = await api(queryUrl(administracao ? '/api/admin/pecas' : '/api/pecas', {...f, pagina, tamanho: 12}));
    if (administracao) S.adminPagina = data.pagina; else S.pagina = data.pagina;
    const title = data.total === 1 ? '1 peça encontrada' : `${data.total} peças encontradas`;
    $('#results-head').innerHTML = `<strong>${title}</strong><span>${I(administracao ? 'part' : 'grid')}${administracao ? 'Organização do catálogo' : 'Fichas técnicas e aplicações'}</span>`;
    if (!data.itens.length) {
        const filtrado = Object.values(f).some(Boolean);
        $('#results').innerHTML = empty(filtrado ? 'Nenhuma peça encontrada' : admin() ? 'Seu catálogo começa aqui.' : 'O catálogo está sendo preparado.', filtrado ? 'Tente outro termo ou ajuste os filtros da consulta.' : admin() ? 'Cadastre a primeira peça e reúna as informações que fazem a diferença no atendimento.' : 'As peças publicadas pela equipe aparecerão nesta consulta.', filtrado ? 'search' : 'part', filtrado ? 'clear-search' : admin() ? 'new-piece' : '', filtrado ? 'Limpar filtros' : 'Adicionar primeira peça');
    } else if (administracao) $('#results').innerHTML = `<div class="panel table-scroll"><table><thead><tr><th>PEÇA / CÓDIGO</th><th>MARCA</th><th>CATEGORIA</th><th>ESTADO</th><th>AÇÕES</th></tr></thead><tbody>${data.itens.map(p => `<tr><td><div class="table-part"><span class="table-thumb">${image(p.imagem)}</span><div><strong>${E(p.nome)}</strong><small>${E(p.codigo || 'Código ainda não informado')}</small></div></div></td><td>${E(p.marca?.nome || 'Não informada')}</td><td>${E(p.categoria?.nome)}</td><td>${badge(p.estado)}</td><td><div class="row-actions"><button class="icon-button" data-action="view-piece" data-id="${p.id}" title="Ver ficha" aria-label="Ver ficha">${I('file')}</button><button class="icon-button" data-action="edit-piece" data-id="${p.id}" title="Editar peça" aria-label="Editar peça">${I('edit')}</button>${p.estado === 'rascunho' ? `<button class="icon-button danger" data-action="delete-piece" data-id="${p.id}" title="Excluir rascunho" aria-label="Excluir rascunho">${I('trash')}</button>` : ''}</div></td></tr>`).join('')}</tbody></table></div>`;
    else $('#results').innerHTML = `<div class="parts-grid">${data.itens.map(p => `<article class="part-card"><div class="part-image">${image(p.imagem)}<span class="category-chip">${E(p.categoria?.nome || 'Peça agrícola')}</span></div><div class="part-body"><span class="part-code">${E(p.codigo || 'CONSULTE AS REFERÊNCIAS')}</span><h3>${E(p.nome)}</h3><div class="part-meta"><span>FABRICANTE<strong>${E(p.marca?.nome || 'Não informado')}</strong></span><span>CATEGORIA<strong>${E(p.categoria?.nome || '—')}</strong></span></div><button class="part-open" data-action="view-piece" data-id="${p.id}">Ver ficha técnica ${I('arrow')}</button></div></article>`).join('')}</div>`;
    $('#pagination').innerHTML = data.total ? `<div class="pagination"><span>Página ${data.pagina} de ${data.paginas}</span><button class="button secondary" data-action="page-prev" ${data.pagina <= 1 ? 'disabled' : ''}>${I('left')} Anterior</button><button class="button secondary" data-action="page-next" ${data.pagina >= data.paginas ? 'disabled' : ''}>Próxima ${I('right')}</button></div>` : '';
}

async function renderUsers(termo = '') {
    S.users = await api(queryUrl('/api/usuarios', {termo}));
    $('#content').innerHTML = pageHead('Usuários', 'Gerencie os acessos e os perfis da sua equipe.', `<button class="button" data-action="new-user">${I('plus')} Novo usuário</button>`) + `<section class="panel"><form class="table-toolbar" data-form="user-search"><div class="search-input">${I('search')}<input name="termo" value="${E(termo)}" placeholder="Pesquisar por nome ou e-mail" aria-label="Pesquisar usuários"></div><button class="button secondary small" type="submit">Pesquisar</button><span class="muted">${S.users.length} cadastro(s)</span></form><div class="table-scroll"><table><thead><tr><th>NOME / E-MAIL</th><th>OCUPAÇÃO</th><th>PERFIL</th><th>ACESSO</th><th>AÇÕES</th></tr></thead><tbody>${S.users.map(u => `<tr><td><strong>${E(u.nome)}</strong><small>${E(u.email)}</small></td><td>${E(L(u.ocupacao))}</td><td>${badge(u.perfil)}</td><td>${badge(u.ativo ? 'ativo' : 'inativo')}</td><td><div class="row-actions"><button class="icon-button" data-action="edit-user" data-id="${u.id}" title="Editar usuário" aria-label="Editar usuário">${I('edit')}</button><button class="icon-button danger" data-action="delete-user" data-id="${u.id}" title="Excluir usuário" aria-label="Excluir usuário">${I('trash')}</button></div></td></tr>`).join('') || '<tr><td colspan="5" class="mini-empty">Nenhum usuário encontrado.</td></tr>'}</tbody></table></div></section>`;
}

function renderApoio() {
    const kinds = {marcas: 'Marcas', categorias: 'Categorias', maquinas: 'Máquinas', origens: 'Origens de códigos'};
    $('#content').innerHTML = pageHead('Cadastros de apoio', 'Uma base organizada facilita a identificação das peças.', `<button class="button" data-action="new-support">${I('plus')} Novo cadastro</button>`) + `<section class="panel"><div class="tabs">${Object.entries(kinds).map(([k, n]) => `<button class="${S.apoioTab === k ? 'active' : ''}" data-action="support-tab" data-kind="${k}">${E(n)}</button>`).join('')}</div><div class="table-toolbar"><div class="search-input">${I('search')}<input data-filter-support placeholder="Pesquisar ${E(kinds[S.apoioTab].toLowerCase())}" aria-label="Filtrar cadastros"></div><span class="muted">${S.apoio[S.apoioTab].length} cadastro(s)</span></div><div class="table-scroll"><table><thead><tr><th>NOME</th><th>${S.apoioTab === 'maquinas' ? 'MODELO / MARCA' : S.apoioTab === 'origens' ? 'TIPO' : 'IDENTIFICAÇÃO'}</th><th>AÇÕES</th></tr></thead><tbody id="support-rows">${supportRows()}</tbody></table></div></section>`;
}

function supportRows(termo = '') {
    return S.apoio[S.apoioTab].filter(o => (o.nome + ' ' + (o.modelo || '')).toLowerCase().includes(termo.toLowerCase())).map(o => `<tr><td><strong>${E(o.nome)}</strong>${S.apoioTab === 'maquinas' ? `<small>${E(o.tipoMaquina)} · ${L(o.estado)}</small>` : ''}</td><td>${E(S.apoioTab === 'maquinas' ? o.modelo + ' · ' + o.marca?.nome : S.apoioTab === 'origens' ? L(o.tipo) : S.apoioTab === 'marcas' ? 'Fabricante / marca' : 'Organização das peças')}</td><td><div class="row-actions"><button class="icon-button" data-action="edit-support" data-id="${o.id}" title="Editar cadastro" aria-label="Editar cadastro">${I('edit')}</button><button class="icon-button danger" data-action="delete-support" data-id="${o.id}" title="Excluir cadastro" aria-label="Excluir cadastro">${I('trash')}</button></div></td></tr>`).join('') || '<tr><td colspan="3" class="mini-empty">Nenhum cadastro encontrado. Use “Novo cadastro” para começar.</td></tr>';
}

function renderPerfil() {
    const u = S.user;
    $('#content').innerHTML = `<div class="page-head"><div><span class="eyebrow">SUA CONTA</span><h1>Meu perfil</h1><p>Mantenha seus dados atualizados e seu acesso protegido.</p></div></div><div class="profile-grid"><section class="panel profile-card"><div class="profile-summary"><span class="avatar">${E(u.nome.split(' ').map(v => v[0]).slice(0, 2).join(''))}</span><div><h3>${E(u.nome)}</h3>${badge(u.perfil)}</div></div><form class="form-stack" data-form="profile">${field('Nome completo', 'nome', u.nome, 'required maxlength="120"')}${field('E-mail', 'email', u.email, 'required', 'email')}${selectField('Ocupação', 'ocupacao', enumOptions(['vendedor', 'agricultor', 'mecanico'], u.ocupacao, admin() ? 'Não informada' : null), admin() ? '' : 'required')}<div class="actions"><button class="button" type="submit">Salvar meus dados</button></div></form></section><section class="panel profile-card"><div class="card-title">${I('shield')}<h3>Alterar senha</h3></div><form class="form-stack" data-form="password">${field('Senha atual', 'atual', '', 'required autocomplete="current-password"', 'password')}${field('Nova senha', 'nova', '', 'required minlength="8" maxlength="1024" autocomplete="new-password"', 'password')}${field('Confirme a nova senha', 'confirmacao', '', 'required autocomplete="new-password"', 'password')}<span class="help">Após a alteração, entre novamente com a nova senha.</span><div class="actions"><button class="button secondary" type="submit">Alterar senha</button></div></form></section></div>`;
}

function dialog(html, classe = '') {
    const scroll = modal.scrollTop;
    modal.className = 'modal ' + classe;
    $('#modal-content').innerHTML = html;
    if (!modal.open) modal.showModal(); else modal.scrollTop = scroll;
}

const modalHead = (title, sub = '') => `<header class="modal-head"><div><h2>${E(title)}</h2>${sub ? `<small>${E(sub)}</small>` : ''}</div><button type="button" class="icon-button" data-action="close-modal" aria-label="Fechar">${I('close')}</button></header>`;

function openUser(id = 0) {
    const u = id ? S.users.find(v => v.id === id) : {
        nome: '',
        email: '',
        ocupacao: 'vendedor',
        perfil: 'usuario',
        ativo: true
    };
    if (!u) return;
    dialog(modalHead(id ? 'Editar usuário' : 'Novo usuário', 'O perfil define as permissões de acesso.') + `<form data-form="user-edit" data-id="${id}"><div class="modal-body form-stack">${field('Nome completo', 'nome', u.nome, 'required maxlength="120"')}${field('E-mail', 'email', u.email, 'required', 'email')}${selectField('Ocupação', 'ocupacao', enumOptions(['vendedor', 'agricultor', 'mecanico'], u.ocupacao, 'Não informada'), u.perfil === 'usuario' ? 'required' : '')}${selectField('Perfil de acesso', 'perfil', enumOptions(['usuario', 'administrador'], u.perfil), 'required')}${field(id ? 'Nova senha (opcional)' : 'Senha', 'senha', '', `${id ? '' : 'required'} minlength="8" maxlength="1024" autocomplete="new-password"`, 'password')}<span class="help">${id ? 'Deixe a senha vazia para manter a atual.' : 'Use pelo menos 8 caracteres.'}</span>${id ? `<label class="check-label"><input type="checkbox" name="ativo" ${u.ativo ? 'checked' : ''}>Conta ativa</label>` : ''}</div><footer class="modal-footer"><button type="button" class="button secondary" data-action="close-modal">Cancelar</button><button type="submit" class="button">Salvar usuário</button></footer></form>`, 'simple');
}

function openSupport(id = 0) {
    const tipo = S.apoioTab, o = id ? S.apoio[tipo].find(v => v.id === id) : {};
    if (!o) return;
    const nomes = {marcas: 'marca', categorias: 'categoria', origens: 'origem', maquinas: 'máquina'};
    let html = field(tipo === 'maquinas' ? 'Nome de exibição' : 'Nome', 'nome', o.nome || '', 'required maxlength="120"');
    if (tipo === 'origens') html += selectField('Tipo da origem', 'tipo', enumOptions(['fabricante', 'fornecedor'], o.tipo || 'fabricante'));
    if (tipo === 'maquinas') html = `<div class="form-grid">${field('Nome de exibição', 'nome', o.nome || '', 'required maxlength="150"')}${field('Modelo', 'modelo', o.modelo || '', 'required maxlength="80"')}${field('Tipo de máquina', 'tipoMaquina', o.tipoMaquina || '', 'required placeholder="Ex.: Trator" maxlength="80"')}${selectField('Marca / fabricante', 'marca', options(S.apoio.marcas, o.marca?.id), 'required')}${field('Ano (opcional)', 'ano', o.ano || '', 'min="1900" max="2200"', 'number')}${selectField('Estado do cadastro', 'estado', enumOptions(['rascunho', 'publicado', 'inativo'], o.estado || 'rascunho'))}${field('Série (opcional)', 'serie', o.serie || '', 'maxlength="100"')}${field('Motor (opcional)', 'motor', o.motor || '', 'maxlength="100"')}${field('Versão (opcional)', 'versao', o.versao || '', 'maxlength="100"')}<div class="field span-2"><label for="f-descricao">Descrição</label><textarea id="f-descricao" name="descricao">${E(o.descricao)}</textarea><span class="help">Para publicar, informe a descrição da máquina.</span></div></div>`;
    dialog(modalHead(`${id ? 'Editar' : 'Nova'} ${nomes[tipo]}`) + `<form data-form="support-edit" data-kind="${tipo}" data-id="${id}"><div class="modal-body form-stack">${html}</div><footer class="modal-footer"><button type="button" class="button secondary" data-action="close-modal">Cancelar</button><button type="submit" class="button">Salvar cadastro</button></footer></form>`, 'simple');
}

async function openEditor(id = 0) {
    if (!S.apoio.categorias.length) {
        closeModal();
        S.apoioTab = 'categorias';
        await navigate('apoio');
        openSupport();
        notify('Crie uma categoria para organizar sua primeira peça.');
        return;
    }
    S.edit = id ? await api('/api/pecas/' + id) : {
        id: 0,
        nome: '',
        descricao: '',
        estado: 'rascunho',
        marca: null,
        categoria: null,
        imagens: [],
        codigos: [],
        especificacoes: [],
        compatibilidades: []
    };
    S.editTab = 'dados';
    S.editDetail = null;
    renderEditor();
}

async function refreshEditor() {
    const tab = S.editTab;
    S.edit = await api('/api/pecas/' + S.edit.id);
    S.editTab = tab;
    S.editDetail = null;
    renderEditor();
}

function renderEditor() {
    const p = S.edit,
        checks = [['Nome da peça', !!p.nome], ['Categoria', !!p.categoria], ['Marca / fabricante', !!p.marca], ['Descrição técnica', !!p.descricao], ['Código de referência', p.codigos.length > 0], ['Imagem da peça', p.imagens.length > 0], ['Especificação técnica', p.especificacoes.length > 0], ['Aplicação confirmada', p.compatibilidades.some(v => v.estado === 'confirmada')]],
        prontas = checks.filter(v => v[1]).length;
    const tabs = {
        dados: 'Dados gerais',
        codigos: 'Códigos',
        imagens: 'Fotos',
        especificacoes: 'Especificações',
        compatibilidades: 'Compatibilidade'
    };
    dialog(modalHead(p.id ? 'Editar peça' : 'Nova peça', p.id ? p.nome : 'Comece pelas informações que você já possui.') + `<div class="editor-layout"><section class="editor-main"><div class="tabs">${Object.entries(tabs).map(([k, n]) => `<button data-action="editor-tab" data-kind="${k}" class="${S.editTab === k ? 'active' : ''}" ${!p.id && k !== 'dados' ? 'disabled' : ''}>${n}</button>`).join('')}</div><div class="subsection">${editorSection()}</div></section><aside class="editor-side"><div class="inline-title"><h3>Preparação da ficha</h3>${badge(p.estado)}</div><p>${prontas === checks.length ? 'Informações necessárias preenchidas.' : `${prontas} de ${checks.length} informações preenchidas.`}</p><progress max="${checks.length}" value="${prontas}"></progress><ul class="checklist">${checks.map(([n, ok]) => `<li class="${ok ? 'done' : ''}">${I('check')}${E(n)}</li>`).join('')}</ul><button class="button wide" data-action="publish-piece" ${!p.id || prontas < checks.length || p.estado === 'publicado' ? 'disabled' : ''}>${I('check')}${p.estado === 'publicado' ? 'Peça publicada' : p.estado === 'inativo' ? 'Publicar novamente' : 'Publicar peça'}</button><p class="help">Confira os códigos e as aplicações antes de publicar.</p></aside></div><footer class="modal-footer"><div>${p.id && p.estado === 'rascunho' ? `<button class="button secondary small" data-action="delete-piece" data-id="${p.id}">${I('trash')}Excluir rascunho</button>` : p.id && p.estado === 'publicado' ? `<button class="button secondary small" data-action="inactivate-piece" data-id="${p.id}">Inativar peça</button>` : ''}</div><button class="button secondary" data-action="close-modal">Concluir</button></footer>`);
}

function editorSection() {
    const p = S.edit, type = S.editTab, detail = S.editDetail;
    if (type === 'dados') return `<h3>Informações da peça</h3><p>Nome e categoria são suficientes para salvar o primeiro rascunho.</p><form data-form="piece-main" class="form-grid">${field('Nome da peça', 'nome', p.nome, 'required maxlength="150" placeholder="Ex.: Filtro de óleo"')}${selectField('Categoria', 'categoria', options(S.apoio.categorias, p.categoria?.id), 'required')}${selectField('Marca / fabricante', 'marca', options(S.apoio.marcas, p.marca?.id, 'Informe quando souber'))}<div></div><div class="field span-2"><label for="f-descricao">Descrição técnica</label><textarea id="f-descricao" name="descricao" placeholder="Descreva o componente e as informações importantes para identificá-lo.">${E(p.descricao)}</textarea></div><div class="span-2 actions"><button class="button" type="submit">${p.id ? 'Salvar informações' : 'Salvar rascunho'} ${I('check')}</button></div></form>${!p.id ? '<div class="notice">Após salvar, adicione códigos, fotos, especificações e compatibilidades nas outras abas.</div>' : ''}`;
    if (type === 'codigos') {
        const c = detail ? p.codigos.find(x => x.id === detail.id) : {};
        return `<h3>${detail ? 'Editar código' : 'Adicionar código de referência'}</h3><p>Guarde a referência original e identifique quem emitiu o código.</p><form data-form="piece-detail" data-kind="codigos" data-id="${c.id || 0}" class="form-grid">${field('Código da peça', 'codigo', c.codigo || '', 'required maxlength="80" placeholder="Preserve letras, zeros e pontuação"')}${selectField('Tipo de código', 'tipo', enumOptions(['original', 'paralelo', 'fornecedor'], c.tipo || 'original'))}${selectField('Marca do código', 'marca', options(S.apoio.marcas, c.marca?.id || p.marca?.id), 'required')}${selectField('Origem / emissor', 'origem', options(S.apoio.origens, c.origem?.id), 'required')}<div class="actions span-2">${detail ? '<button type="button" class="button secondary" data-action="cancel-detail">Cancelar edição</button>' : ''}<button class="button" type="submit">${detail ? 'Salvar código' : 'Adicionar código'}</button></div></form>${!S.apoio.origens.length ? '<div class="notice">Cadastre o emissor em “Cadastros de apoio → Origens de códigos”.</div>' : ''}<div class="detail-list">${p.codigos.map(c => detailRow(`<strong>${E(c.codigo)}</strong><small>${E(c.marca?.nome)} · ${E(c.origem?.nome)} · ${L(c.tipo)}</small>`, 'codigos', c.id)).join('') || '<div class="mini-empty">A peça ainda não possui códigos.</div>'}</div>`;
    }
    if (type === 'especificacoes') {
        const e = detail ? p.especificacoes.find(x => x.id === detail.id) : {};
        return `<h3>${detail ? 'Editar especificação' : 'Adicionar especificação'}</h3><p>Informe características que ajudem a conferir a peça, como diâmetro, material ou capacidade.</p><form data-form="piece-detail" data-kind="especificacoes" data-id="${e.id || 0}" class="form-grid">${field('Característica', 'nome', e.nome || '', 'required maxlength="80" placeholder="Ex.: Diâmetro externo"')}${field('Valor', 'valor', e.valor || '', 'required maxlength="255" placeholder="Ex.: 80"')}${field('Unidade (quando aplicável)', 'unidade', e.unidade || '', 'maxlength="30" placeholder="Ex.: mm"')}<div></div><div class="actions span-2">${detail ? '<button type="button" class="button secondary" data-action="cancel-detail">Cancelar edição</button>' : ''}<button class="button" type="submit">${detail ? 'Salvar especificação' : 'Adicionar especificação'}</button></div></form><div class="detail-list">${p.especificacoes.map(e => detailRow(`<strong>${E(e.nome)}</strong><small>${E(e.valor)} ${E(e.unidade)}</small>`, 'especificacoes', e.id)).join('') || '<div class="mini-empty">Nenhuma especificação cadastrada.</div>'}</div>`;
    }
    if (type === 'imagens') {
        const i = detail ? p.imagens.find(x => x.id === detail.id) : {};
        return `<h3>${detail ? 'Editar foto' : 'Adicionar foto da peça'}</h3><p>Use uma foto nítida que mostre o componente e seus detalhes.</p><form data-form="piece-photo" data-id="${i.id || 0}" class="form-stack">${!detail ? '<div class="upload-box"><label for="photo-file"><strong>Enviar do computador</strong></label><span class="help">PNG, JPEG ou WebP · Até 8 MB</span><input id="photo-file" type="file" name="arquivo" accept="image/png,image/jpeg,image/webp"></div><div class="divider">OU USE UM LINK DE IMAGEM</div>' : ''}${field('Endereço da imagem', 'url', i.url || '', 'placeholder="https://…" maxlength="2048"')}${field('Descrição da foto (opcional)', 'descricao', i.descricao || '', 'maxlength="255" placeholder="Ex.: Vista frontal da peça"')}<div class="actions">${detail ? '<button class="button secondary" type="button" data-action="cancel-detail">Cancelar edição</button>' : ''}<button class="button" type="submit">${detail ? 'Salvar foto' : 'Adicionar foto'}</button></div></form><div class="detail-list">${p.imagens.map(i => detailRow(`<div class="detail-photo">${image(i.url)}<div><strong>${E(i.descricao || 'Foto da peça')}</strong><small>Imagem vinculada ao cadastro</small></div></div>`, 'imagens', i.id)).join('') || '<div class="mini-empty">Nenhuma foto cadastrada.</div>'}</div>`;
    }
    const c = detail ? p.compatibilidades.find(x => x.maquina.id === detail.id) : {};
    return `<h3>${detail ? 'Editar aplicação' : 'Vincular uma máquina'}</h3><p>Confirme a aplicação com uma fonte técnica e registre as restrições informadas.</p><form data-form="piece-compat" class="form-grid">${selectField('Máquina / modelo', 'maquina', options(S.apoio.maquinas.map(m => ({
        ...m,
        nome: m.marca?.nome + ' · ' + m.modelo
    })), c.maquina?.id), 'required ' + (detail ? 'disabled' : ''))}${selectField('Situação da aplicação', 'estado', enumOptions(['pendente', 'confirmada'], c.estado || 'pendente'))}<div class="field span-2"><label for="f-fonte">Fonte da informação</label><input id="f-fonte" name="fonte" value="${E(c.fonte)}" maxlength="500" placeholder="Ex.: Catálogo do fabricante, edição e página" ${c.estado === 'confirmada' ? 'required' : ''}></div>${field('Ano inicial (opcional)', 'anoInicio', c.anoInicio || '', 'min="1900" max="2200"', 'number')}${field('Ano final (opcional)', 'anoFim', c.anoFim || '', 'min="1900" max="2200"', 'number')}${field('Restrição de série', 'restricaoSerie', c.restricaoSerie || '', 'maxlength="150"')}${field('Restrição de motor', 'restricaoMotor', c.restricaoMotor || '', 'maxlength="150"')}${field('Restrição de versão', 'restricaoVersao', c.restricaoVersao || '', 'maxlength="150"')}<div></div><div class="field span-2"><label for="f-observacoes">Observações</label><textarea id="f-observacoes" name="observacoes">${E(c.observacoes)}</textarea></div><div class="actions span-2">${detail ? '<button class="button secondary" type="button" data-action="cancel-detail">Cancelar edição</button>' : ''}<button class="button" type="submit">${detail ? 'Salvar aplicação' : 'Vincular máquina'}</button></div></form><div class="detail-list">${p.compatibilidades.map(c => detailRow(`<strong>${E(c.maquina.marca?.nome)} · ${E(c.maquina.modelo)}</strong><small>${L(c.estado)}${c.anoInicio || c.anoFim ? ' · ' + (c.anoInicio || '…') + ' a ' + (c.anoFim || '…') : ''}</small>`, 'compatibilidades', c.maquina.id)).join('') || '<div class="mini-empty">Nenhuma máquina vinculada a esta peça.</div>'}</div>`;
}

function detailRow(content, type, id) {
    return `<div class="detail-row"><div>${content}</div><div class="row-actions"><button class="icon-button" data-action="edit-detail" data-kind="${type}" data-id="${id}" title="Editar" aria-label="Editar detalhe">${I('edit')}</button><button class="icon-button danger" data-action="delete-detail" data-kind="${type}" data-id="${id}" title="Remover" aria-label="Remover detalhe">${I('trash')}</button></div></div>`;
}

async function openFicha(id) {
    const p = await api('/api/pecas/' + id);
    const restricoes = c => [c.anoInicio || c.anoFim ? `Ano: ${c.anoInicio || 'sem início informado'} a ${c.anoFim || 'sem término informado'}` : '', c.restricaoSerie ? 'Série: ' + c.restricaoSerie : '', c.restricaoMotor ? 'Motor: ' + c.restricaoMotor : '', c.restricaoVersao ? 'Versão: ' + c.restricaoVersao : '', c.observacoes].filter(Boolean).join('\n');
    dialog(modalHead('Ficha técnica', p.categoria?.nome || 'Identificação e compatibilidade') + `<div class="modal-body"><div class="ficha-top"><div><div class="ficha-main-image">${image(p.imagens[0]?.url)}</div>${p.imagens.length > 1 ? `<div class="photo-strip">${p.imagens.map(i => image(i.url)).join('')}</div>` : ''}</div><div class="ficha-title"><span class="eyebrow">${E(p.marca?.nome || 'PEÇA AGRÍCOLA')}</span><h2>${E(p.nome)}</h2>${admin() ? badge(p.estado) : ''}<p class="ficha-description">${E(p.descricao || 'Descrição ainda não informada.')}</p><div class="part-meta"><span>CATEGORIA<strong>${E(p.categoria?.nome)}</strong></span>${admin() && p.autor ? `<span>CADASTRO POR<strong>${E(p.autor.nome)}</strong></span>` : ''}</div>${admin() ? `<button class="button secondary small" data-action="edit-piece" data-id="${p.id}">${I('edit')}Editar cadastro</button>` : ''}</div></div><div class="ficha-sections"><section class="ficha-section"><h3>${I('link')}Códigos de referência</h3>${p.codigos.map(c => `<div class="detail-row"><div><strong>${E(c.codigo)}</strong><small>${E(c.marca?.nome)} · ${E(c.origem?.nome)} · ${L(c.tipo)}</small></div><button class="icon-button" data-action="copy-code" data-code="${E(c.codigo)}" title="Copiar código" aria-label="Copiar código">${I('copy')}</button></div>`).join('') || '<p class="muted">Nenhum código informado.</p>'}</section><section class="ficha-section"><h3>${I('file')}Especificações</h3><table class="spec-table"><tbody>${p.especificacoes.map(e => `<tr><td>${E(e.nome)}</td><td>${E(e.valor)} ${E(e.unidade)}</td></tr>`).join('') || '<tr><td>Nenhuma especificação informada.</td></tr>'}</tbody></table></section><section class="ficha-section span-2"><h3>${I('tractor')}Aplicações e compatibilidades</h3>${p.compatibilidades.map(c => `<article class="compat-card"><div class="inline-title"><strong>${E(c.maquina.marca?.nome)} · ${E(c.maquina.modelo)}</strong>${badge(c.estado)}</div><p>${E(restricoes(c) || 'Sem restrições adicionais informadas.')}</p>${c.fonte ? `<div class="source">Fonte: ${E(c.fonte)}</div>` : '<div class="source">Aguardando conferência da aplicação.</div>'}</article>`).join('') || '<p class="muted">Nenhuma aplicação confirmada disponível.</p>'}</section></div></div><footer class="modal-footer"><small>Confira a aplicação e as restrições antes de escolher a peça.</small><button class="button secondary" data-action="close-modal">Fechar ficha</button></footer>`);
}

function confirmAction(title, text, ok = 'Excluir') {
    const d = $('#confirm');
    $('#confirm-title').textContent = title;
    $('#confirm-text').textContent = text;
    $('#confirm-ok').textContent = ok;
    d.returnValue = 'cancel';
    return new Promise(resolve => {
        d.addEventListener('close', () => resolve(d.returnValue === 'ok'), {once: true});
        d.showModal();
    });
}


async function refreshPage() {
    if (S.page === 'catalogo' || S.page === 'pecas') await loadPecas(S.page === 'pecas'); else if (S.page === 'usuarios') await renderUsers(); else if (S.page === 'apoio') {
        await refreshApoio();
        renderApoio();
    }
}

async function onSubmit(form) {
    const kind = form.dataset.form, b = formData(form), id = Number(form.dataset.id || 0);
    if (['login', 'registro', 'setup'].includes(kind)) {
        $('#auth-error').hidden = true;
        if (kind !== 'login' && b.senha !== b.confirmacao) throw new Error('As senhas precisam ser iguais.');
        if (kind === 'login') {
            const r = await api('/api/login', {method: 'POST', data: b});
            await enterApp(r.usuario);
        } else if (kind === 'setup') {
            const r = await api('/api/configuracao', {method: 'POST', data: b, setup: S.setup});
            S.setup = null;
            await enterApp(r.usuario);
            notify('Seu acesso administrativo está pronto. Bem-vindo!');
        } else {
            await api('/api/registro', {method: 'POST', data: b});
            const r = await api('/api/login', {method: 'POST', data: {email: b.email, senha: b.senha}});
            await enterApp(r.usuario);
            notify('Conta criada. Você já pode consultar o catálogo.');
        }
        return;
    }
    if (kind === 'catalog-search' || kind === 'admin-search') {
        if (kind === 'admin-search') {
            S.adminQuery = b;
            S.adminPagina = 1;
        } else {
            S.query = b;
            S.pagina = 1;
        }
        await loadPecas(kind === 'admin-search');
        return;
    }
    if (kind === 'user-search') {
        await renderUsers(b.termo);
        return;
    }
    if (kind === 'profile') {
        S.user = await api('/api/perfil', {method: 'PUT', data: b});
        $('#user-name').textContent = S.user.nome;
        renderPerfil();
        notify('Seus dados foram atualizados.');
        return;
    }
    if (kind === 'password') {
        if (b.nova !== b.confirmacao) throw new Error('A confirmação da nova senha está diferente.');
        await api('/api/perfil/senha', {method: 'POST', data: b});
        showAuth('login');
        notify('Senha alterada. Entre com a nova senha.');
        return;
    }
    if (kind === 'user-edit') {
        b.ativo = id ? form.elements.ativo.checked : true;
        b.ocupacao = b.ocupacao || null;
        const own = id && id === S.user.id;
        await api(id ? '/api/usuarios/' + id : '/api/usuarios', {method: id ? 'PUT' : 'POST', data: b});
        closeModal();
        if (own) {
            showAuth('login');
            notify('Conta atualizada. Entre novamente para continuar.');
        } else {
            await renderUsers();
            notify('Usuário salvo.');
        }
        return;
    }
    if (kind === 'support-edit') {
        const type = form.dataset.kind;
        if (type === 'maquinas') {
            b.marca = num(b.marca);
            b.ano = num(b.ano);
        }
        await api('/api/' + type + (id ? '/' + id : ''), {method: id ? 'PUT' : 'POST', data: b});
        closeModal();
        await refreshApoio();
        renderApoio();
        notify('Cadastro salvo.');
        return;
    }
    if (kind === 'piece-main') {
        b.categoria = num(b.categoria);
        b.marca = num(b.marca);
        const p = await api(S.edit.id ? '/api/pecas/' + S.edit.id : '/api/pecas', {
            method: S.edit.id ? 'PUT' : 'POST',
            data: b
        });
        S.edit = await api('/api/pecas/' + p.id);
        S.editDetail = null;
        renderEditor();
        await refreshPage();
        notify('Informações da peça salvas.');
        return;
    }
    if (kind === 'piece-detail') {
        const collection = form.dataset.kind;
        if (collection === 'codigos') {
            b.marca = num(b.marca);
            b.origem = num(b.origem);
        }
        await api('/api/pecas/' + S.edit.id + '/' + collection + (id ? '/' + id : ''), {
            method: id ? 'PUT' : 'POST',
            data: b
        });
        await refreshEditor();
        await refreshPage();
        notify('Informação salva.');
        return;
    }
    if (kind === 'piece-photo') {
        const file = form.elements.arquivo?.files[0];
        if (file) {
            if (file.size > 8 * 1024 * 1024) throw new Error('A imagem deve ter até 8 MB.');
            await api(queryUrl('/api/pecas/' + S.edit.id + '/imagens/upload', {descricao: b.descricao}), {
                method: 'POST',
                file
            });
        } else {
            if (!b.url?.trim()) throw new Error('Escolha uma foto do computador ou informe um link de imagem.');
            await api('/api/pecas/' + S.edit.id + '/imagens' + (id ? '/' + id : ''), {
                method: id ? 'PUT' : 'POST',
                data: {url: b.url, descricao: b.descricao}
            });
        }
        await refreshEditor();
        await refreshPage();
        notify('Foto salva.');
        return;
    }
    if (kind === 'piece-compat') {
        b.maquina = Number(form.elements.maquina.value);
        b.anoInicio = num(b.anoInicio);
        b.anoFim = num(b.anoFim);
        if (b.anoInicio && b.anoFim && b.anoInicio > b.anoFim) throw new Error('O ano inicial deve ser menor ou igual ao ano final.');
        await api('/api/pecas/' + S.edit.id + '/compatibilidades', {method: 'POST', data: b});
        await refreshEditor();
        notify('Aplicação salva.');
        return;
    }
}

async function action(name, el) {
    const id = Number(el.dataset.id || 0), type = el.dataset.kind;
    switch (name) {
        case 'auth-login':
            showAuth('login');
            return;
        case 'auth-registro':
            showAuth('registro');
            return;
        case 'close-modal':
            closeModal();
            return;
        case 'retry':
            await navigate(S.page);
            return;
        case 'clear-search':
            if (S.page === 'pecas') {
                S.adminQuery = {};
                S.adminPagina = 1;
            } else {
                S.query = {};
                S.pagina = 1;
            }
            await renderPecas(S.page === 'pecas');
            return;
        case 'page-next':
            if (S.page === 'pecas') S.adminPagina++; else S.pagina++;
            await loadPecas(S.page === 'pecas');
            return;
        case 'page-prev':
            if (S.page === 'pecas') S.adminPagina--; else S.pagina--;
            await loadPecas(S.page === 'pecas');
            return;
        case 'new-piece':
            await openEditor();
            return;
        case 'edit-piece':
            await openEditor(id);
            return;
        case 'view-piece':
            await openFicha(id);
            return;
        case 'editor-tab':
            S.editTab = type;
            S.editDetail = null;
            renderEditor();
            return;
        case 'edit-detail':
            S.editTab = type;
            S.editDetail = {id};
            renderEditor();
            return;
        case 'cancel-detail':
            S.editDetail = null;
            renderEditor();
            return;
        case 'publish-piece':
            if (await confirmAction('Publicar esta peça?', 'Confirme que os códigos, as especificações e as aplicações foram conferidos. A peça ficará disponível na consulta.', 'Publicar')) {
                await api('/api/pecas/' + S.edit.id + '/publicar', {method: 'POST'});
                await refreshEditor();
                await refreshPage();
                notify('Peça publicada no catálogo.');
            }
            return;
        case 'inactivate-piece':
            if (await confirmAction('Inativar esta peça?', 'A peça sairá da consulta, e seus dados e vínculos serão preservados.', 'Inativar')) {
                await api('/api/pecas/' + id + '/inativar', {method: 'POST'});
                if (modal.open && S.edit?.id === id) await refreshEditor();
                await refreshPage();
                notify('Peça inativada.');
            }
            return;
        case 'delete-piece':
            if (await confirmAction('Excluir este rascunho?', 'O rascunho e seus detalhes serão removidos. Cadastros com referências externas precisam ser preservados.')) {
                await api('/api/pecas/' + id, {method: 'DELETE'});
                closeModal();
                S.edit = null;
                await refreshPage();
                notify('Rascunho excluído.');
            }
            return;
        case 'delete-detail':
            if (await confirmAction('Remover esta informação?', 'A peça publicada precisa manter todos os dados exigidos. A remoção será recusada se deixar a ficha incompleta.', 'Remover')) {
                await api('/api/pecas/' + S.edit.id + '/' + type + '/' + id, {method: 'DELETE'});
                await refreshEditor();
                await refreshPage();
                notify('Informação removida.');
            }
            return;
        case 'copy-code':
            if (navigator.clipboard) {
                await navigator.clipboard.writeText(el.dataset.code);
                notify('Código copiado.');
            } else notify('Copie a referência exibida na ficha.');
            return;
        case 'new-user':
            openUser();
            return;
        case 'edit-user':
            openUser(id);
            return;
        case 'delete-user':
            if (await confirmAction('Excluir este usuário?', 'A conta e seus favoritos serão removidos. Os cadastros técnicos de peças serão preservados.')) {
                const own = id === S.user.id;
                await api('/api/usuarios/' + id, {method: 'DELETE'});
                if (own) showAuth('login'); else await renderUsers();
                notify('Usuário excluído.');
            }
            return;
        case 'support-tab':
            S.apoioTab = type;
            renderApoio();
            return;
        case 'new-support':
            openSupport();
            return;
        case 'edit-support':
            openSupport(id);
            return;
        case 'delete-support':
            if (await confirmAction('Excluir este cadastro?', 'A exclusão será permitida apenas quando o cadastro não estiver em uso por outros registros.')) {
                await api('/api/' + S.apoioTab + '/' + id, {method: 'DELETE'});
                await refreshApoio();
                renderApoio();
                notify('Cadastro excluído.');
            }
            return;
    }
}

document.addEventListener('submit', event => {
    const form = event.target;
    if (!form.matches('form[data-form]')) return;
    event.preventDefault();
    const buttons = $$('button[type=submit]', form);
    if (buttons.some(b => b.disabled)) return;
    buttons.forEach(b => b.disabled = true);
    Promise.resolve().then(() => onSubmit(form)).catch(e => {
        if (['login', 'registro', 'setup'].includes(form.dataset.form)) {
            $('#auth-error').textContent = e.message;
            $('#auth-error').hidden = false;
        } else notify(e.message, true);
    }).finally(() => buttons.forEach(b => b.disabled = false));
});
document.addEventListener('click', event => {
    const nav = event.target.closest('[data-page]');
    if (nav) {
        event.preventDefault();
        navigate(nav.dataset.page).catch(e => notify(e.message, true));
        return;
    }
    const b = event.target.closest('[data-action]');
    if (!b || b.disabled) return;
    event.preventDefault();
    Promise.resolve().then(() => action(b.dataset.action, b)).catch(e => notify(e.message, true));
});
document.addEventListener('input', e => {
    if (e.target.matches('[data-filter-support]')) $('#support-rows').innerHTML = supportRows(e.target.value);
});
document.addEventListener('change', e => {
    const f = e.target.closest('form');
    if (f?.dataset.form === 'piece-compat' && e.target.name === 'estado') f.elements.fonte.required = e.target.value === 'confirmada';
    if (f?.dataset.form === 'user-edit' && e.target.name === 'perfil') f.elements.ocupacao.required = e.target.value === 'usuario';
});
document.addEventListener('error', e => {
    if (e.target.matches?.('img[data-photo]')) {
        e.target.hidden = true;
        const fallback = e.target.nextElementSibling;
        if (fallback) fallback.hidden = false;
    }
}, true);
$('#logout').addEventListener('click', () => api('/api/logout', {method: 'POST'}).then(() => showAuth('login')).catch(e => notify(e.message, true)));
$('#menu-toggle').addEventListener('click', () => $('#sidebar').classList.toggle('open'));

async function init() {
    const hash = new URLSearchParams(location.hash.slice(1));
    S.setup = hash.get('configurar');
    if (S.setup) history.replaceState(null, '', location.pathname);
    try {
        const status = await api('/api/status');
        if (S.setup && status.configuracaoPendente) {
            showAuth('setup');
            return;
        }
        let user = null;
        try {
            user = await api('/api/perfil');
        } catch (e) {
            if (e.status !== 401) throw e;
        }
        if (user) await enterApp(user); else {
            showAuth('login');
            if (status.configuracaoPendente) {
                $('#auth-error').className = 'notice';
                $('#auth-error').textContent = 'Primeiro acesso: abra o endereço de configuração exibido ao iniciar o sistema no IntelliJ.';
                $('#auth-error').hidden = false;
            }
        }
    } catch (e) {
        showAuth('login');
        $('#auth-error').className = 'notice error';
        $('#auth-error').textContent = e.message;
        $('#auth-error').hidden = false;
    } finally {
        $('#boot').hidden = true;
    }
}

init();
