/*
 * Interface de consumo da API.
 *
 * JS puro, sem framework nem build: a pagina e servida pelo proprio Spring
 * Boot, na mesma origem da API. Um comando sobe tudo e nao existe CORS no meio.
 *
 * Sao duas telas na mesma pagina -- login e aplicacao. Quem manda em qual
 * aparece e a existencia de um token valido, nunca um clique de navegacao.
 */

(function () {
  'use strict';

  const CHAVE_SESSAO = 'pessoas-api:sessao';

  const el = (id) => document.getElementById(id);

  const tela = {
    telaLogin: el('telaLogin'),
    telaApp: el('telaApp'),
    sessao: el('sessao'),
    sessaoUsuario: el('sessaoUsuario'),
    sessaoPerfil: el('sessaoPerfil'),
    btnSair: el('btnSair'),
    formLogin: el('formLogin'),
    usuario: el('usuario'),
    senha: el('senha'),
    btnEntrar: el('btnEntrar'),
    loginErro: el('loginErro'),
    formCadastro: el('formCadastro'),
    documento: el('documento'),
    filtro: el('filtro'),
    btnAtualizar: el('btnAtualizar'),
    corpoTabela: el('corpoTabela'),
    nacionalidadeVazia: el('nacionalidadeVazia'),
    previsao: el('previsao'),
    previsaoNome: el('previsaoNome'),
    previsaoPais: el('previsaoPais'),
    previsaoBarra: el('previsaoBarra'),
    previsaoProbabilidade: el('previsaoProbabilidade'),
    previsaoIso: el('previsaoIso'),
    previsaoAlternativas: el('previsaoAlternativas'),
    aviso: el('aviso')
  };

  let sessao = carregarSessao();

  /* ---------------- sessao ---------------- */

  function carregarSessao() {
    try {
      const bruto = sessionStorage.getItem(CHAVE_SESSAO);
      return bruto ? JSON.parse(bruto) : null;
    } catch (e) {
      return null;
    }
  }

  function salvarSessao(dados) {
    sessao = dados;
    try {
      if (dados) {
        sessionStorage.setItem(CHAVE_SESSAO, JSON.stringify(dados));
      } else {
        sessionStorage.removeItem(CHAVE_SESSAO);
      }
    } catch (e) {
      /* navegador sem storage: a sessao vive so em memoria */
    }
    desenharSessao();
  }

  function ehAdmin() {
    return !!sessao && Array.isArray(sessao.perfis) && sessao.perfis.includes('ROLE_ADMIN');
  }

  /**
   * Ponto unico que decide qual tela aparece. Perdeu o token -- por logout ou
   * por 401 do servidor -- volta para o login sem passo intermediario.
   */
  function desenharSessao() {
    const autenticado = !!sessao;

    tela.telaLogin.hidden = autenticado;
    tela.telaApp.hidden = !autenticado;
    tela.sessao.hidden = !autenticado;

    if (autenticado) {
      tela.sessaoUsuario.textContent = sessao.usuario;
      tela.sessaoPerfil.textContent = ehAdmin() ? 'admin' : 'user';
    } else {
      tela.senha.value = '';
      tela.usuario.focus();
    }
  }

  /* ---------------- chamada http ---------------- */

  async function chamar(caminho, opcoes) {
    const config = Object.assign({ headers: {} }, opcoes);
    config.headers = Object.assign({ Accept: 'application/json' }, config.headers);

    if (sessao && sessao.token) {
      config.headers.Authorization = 'Bearer ' + sessao.token;
    }
    if (config.body) {
      config.headers['Content-Type'] = 'application/json';
    }

    const resposta = await fetch(caminho, config);

    if (resposta.status === 204) {
      return null;
    }

    const texto = await resposta.text();
    const corpo = texto ? JSON.parse(texto) : null;

    if (!resposta.ok) {
      // Token expirado ou revogado: derruba a sessao e volta para o login.
      if (resposta.status === 401 && sessao) {
        salvarSessao(null);
        mostrarErroDeLogin('Sua sessao expirou. Entre de novo.');
      }
      const erro = new Error((corpo && corpo.detail) || 'Falha na requisicao');
      erro.status = resposta.status;
      erro.corpo = corpo;
      throw erro;
    }

    return corpo;
  }

  /* ---------------- avisos ---------------- */

  let timerAviso = null;

  function avisar(mensagem, tipo) {
    tela.aviso.textContent = mensagem;
    tela.aviso.className = 'aviso' + (tipo ? ' aviso--' + tipo : '');
    tela.aviso.hidden = false;

    clearTimeout(timerAviso);
    timerAviso = setTimeout(() => { tela.aviso.hidden = true; }, 4500);
  }

  /* ---------------- login ---------------- */

  function mostrarErroDeLogin(mensagem) {
    tela.loginErro.textContent = mensagem || '';
  }

  tela.formLogin.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    mostrarErroDeLogin('');
    tela.btnEntrar.disabled = true;

    try {
      const dados = await chamar('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          usuario: tela.usuario.value.trim(),
          senha: tela.senha.value
        })
      });

      salvarSessao(dados);
      avisar('Autenticado como ' + dados.usuario + '.', 'ok');
      carregarLista();

    } catch (erro) {
      mostrarErroDeLogin(erro.status === 401
        ? 'Usuario ou senha invalidos.'
        : erro.message);

    } finally {
      tela.btnEntrar.disabled = false;
    }
  });

  tela.usuario.addEventListener('input', () => mostrarErroDeLogin(''));
  tela.senha.addEventListener('input', () => mostrarErroDeLogin(''));

  tela.btnSair.addEventListener('click', () => {
    salvarSessao(null);
    mostrarErroDeLogin('');
    limparPrevisao();
    limparErrosDoFormulario();
    tela.formCadastro.reset();
    tela.filtro.value = '';
    tela.corpoTabela.innerHTML = '';
    avisar('Sessao encerrada.');
  });

  /* ---------------- cadastro ---------------- */

  tela.documento.addEventListener('input', (evento) => {
    const digitos = evento.target.value.replace(/\D/g, '').slice(0, 11);
    let saida = digitos;

    if (digitos.length > 9) {
      saida = digitos.replace(/(\d{3})(\d{3})(\d{3})(\d{1,2})/, '$1.$2.$3-$4');
    } else if (digitos.length > 6) {
      saida = digitos.replace(/(\d{3})(\d{3})(\d{1,3})/, '$1.$2.$3');
    } else if (digitos.length > 3) {
      saida = digitos.replace(/(\d{3})(\d{1,3})/, '$1.$2');
    }

    evento.target.value = saida;
  });

  tela.formCadastro.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    limparErrosDoFormulario();

    const corpo = {
      documento: tela.documento.value.trim(),
      nome: el('nome').value.trim(),
      sobrenome: el('sobrenome').value.trim(),
      email: el('email').value.trim()
    };

    try {
      const pessoa = await chamar('/registrarName', {
        method: 'POST',
        body: JSON.stringify(corpo)
      });

      tela.formCadastro.reset();
      avisar(pessoa.nomeCompleto + ' foi registrado.', 'ok');
      carregarLista();

    } catch (erro) {
      // O backend devolve o mapa 'erros' campo a campo. A tela so espelha.
      if (erro.corpo && erro.corpo.erros) {
        mostrarErrosDoFormulario(erro.corpo.erros);
        avisar('Confira os campos destacados.', 'erro');
      } else {
        avisar(erro.message, 'erro');
      }
    }
  });

  function mostrarErrosDoFormulario(erros) {
    Object.keys(erros).forEach((campo) => {
      const alvo = document.querySelector('[data-erro="' + campo + '"]');
      if (alvo) {
        alvo.textContent = erros[campo];
        alvo.closest('.campo').classList.add('campo--invalido');
      }
    });
  }

  function limparErrosDoFormulario() {
    document.querySelectorAll('.campo__erro').forEach((n) => { n.textContent = ''; });
    document.querySelectorAll('.campo--invalido').forEach((n) => n.classList.remove('campo--invalido'));
  }

  /* ---------------- lista ---------------- */

  async function carregarLista() {
    if (!sessao) {
      return;
    }

    const filtro = tela.filtro.value.trim();
    const caminho = filtro ? '/list?nome=' + encodeURIComponent(filtro) : '/list';

    try {
      const pessoas = await chamar(caminho);
      desenharLista(pessoas);
    } catch (erro) {
      tela.corpoTabela.innerHTML = linhaAviso(erro.message);
    }
  }

  function desenharLista(pessoas) {
    if (!pessoas || pessoas.length === 0) {
      tela.corpoTabela.innerHTML = linhaAviso('Nenhuma pessoa registrada com esse filtro.');
      return;
    }

    tela.corpoTabela.innerHTML = '';

    pessoas.forEach((pessoa) => {
      const linha = document.createElement('tr');

      linha.appendChild(celula(pessoa.documento, 'mono'));
      linha.appendChild(celula(pessoa.nomeCompleto));
      linha.appendChild(celula(pessoa.email));

      const acoes = document.createElement('td');
      acoes.className = 'col-acoes';

      const grupo = document.createElement('div');
      grupo.className = 'acoes';

      grupo.appendChild(botao('prever', 'btn btn--mini', () => preverNacionalidade(pessoa)));

      const excluir = botao('excluir', 'btn btn--mini btn--perigo', () => excluirPessoa(pessoa));
      if (!ehAdmin()) {
        excluir.disabled = true;
        excluir.title = 'Somente o perfil ADMIN pode excluir';
      }
      grupo.appendChild(excluir);

      acoes.appendChild(grupo);
      linha.appendChild(acoes);

      tela.corpoTabela.appendChild(linha);
    });
  }

  function celula(texto, classe) {
    const td = document.createElement('td');
    td.textContent = texto;
    if (classe) td.className = classe;
    return td;
  }

  function botao(rotulo, classe, aoClicar) {
    const b = document.createElement('button');
    b.type = 'button';
    b.className = classe;
    b.textContent = rotulo;
    b.addEventListener('click', aoClicar);
    return b;
  }

  function linhaAviso(texto) {
    const td = document.createElement('td');
    td.colSpan = 4;
    td.className = 'vazio';
    td.textContent = texto;

    const tr = document.createElement('tr');
    tr.appendChild(td);
    return tr.outerHTML;
  }

  /* ---------------- exclusao ---------------- */

  async function excluirPessoa(pessoa) {
    if (!confirm('Excluir ' + pessoa.nomeCompleto + ' em definitivo?')) {
      return;
    }

    try {
      await chamar('/list/' + encodeURIComponent(pessoa.documento), { method: 'DELETE' });
      avisar(pessoa.nomeCompleto + ' foi excluido.', 'ok');
      limparPrevisao();
      carregarLista();

    } catch (erro) {
      avisar(erro.status === 403
        ? 'Seu perfil nao pode excluir. Entre como admin.'
        : erro.message, 'erro');
    }
  }

  /* ---------------- nacionalidade ---------------- */

  async function preverNacionalidade(pessoa) {
    try {
      const previsao = await chamar('/findNacionalityByPerson/' + encodeURIComponent(pessoa.documento));
      desenharPrevisao(previsao);

    } catch (erro) {
      limparPrevisao();
      avisar(erro.status === 502
        ? 'A API de nacionalidade nao respondeu. Tente de novo em instantes.'
        : erro.message, 'erro');
    }
  }

  function desenharPrevisao(previsao) {
    tela.nacionalidadeVazia.hidden = true;
    tela.previsao.hidden = false;

    tela.previsaoNome.textContent = previsao.nomeConsultado;

    if (!previsao.nacionalidade) {
      tela.previsaoPais.textContent = 'sem previsão';
      tela.previsaoBarra.style.width = '0%';
      tela.previsaoProbabilidade.textContent = 'A API não tem dado para esse nome.';
      tela.previsaoIso.textContent = '—';
      tela.previsaoAlternativas.innerHTML = '';
      return;
    }

    const percentual = Math.round((previsao.probabilidade || 0) * 100);

    tela.previsaoPais.textContent = previsao.nacionalidade;
    tela.previsaoBarra.style.width = percentual + '%';
    tela.previsaoProbabilidade.textContent = percentual + '% de probabilidade';
    tela.previsaoIso.textContent = previsao.codigoIso;

    desenharAlternativas(previsao.alternativas);
  }

  function desenharAlternativas(alternativas) {
    tela.previsaoAlternativas.innerHTML = '';

    if (!alternativas || alternativas.length === 0) {
      return;
    }

    const bloco = document.createElement('div');
    bloco.className = 'alternativas';

    const titulo = document.createElement('h3');
    titulo.textContent = 'Outras possibilidades';
    bloco.appendChild(titulo);

    alternativas.forEach((item) => {
      const linha = document.createElement('div');
      linha.className = 'alternativa';

      const pais = document.createElement('span');
      pais.textContent = item.nacionalidade;

      const valor = document.createElement('span');
      valor.textContent = Math.round((item.probabilidade || 0) * 100) + '%';

      linha.appendChild(pais);
      linha.appendChild(valor);
      bloco.appendChild(linha);
    });

    tela.previsaoAlternativas.appendChild(bloco);
  }

  function limparPrevisao() {
    tela.previsao.hidden = true;
    tela.nacionalidadeVazia.hidden = false;
    tela.previsaoAlternativas.innerHTML = '';
  }

  /* ---------------- eventos gerais ---------------- */

  tela.btnAtualizar.addEventListener('click', carregarLista);

  tela.filtro.addEventListener('keydown', (evento) => {
    if (evento.key === 'Enter') {
      evento.preventDefault();
      carregarLista();
    }
  });

  desenharSessao();
  if (sessao) {
    carregarLista();
  }
})();
