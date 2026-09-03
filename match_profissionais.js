const XLSX = require('xlsx');
const { createConnection } = require('node:net');
const http = require('node:http');

// Normaliza nome: maiúsculo, sem acento, sem espaços duplos
function norm(s) {
  if (!s) return '';
  return s.toUpperCase()
    .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/\s+/g, ' ')
    .trim();
}

// Lê planilha do PDF
const wb = XLSX.readFile('C:/Users/sergioa/Downloads/Prof_NTSMS_6970451.xlsx');
const ws = wb.Sheets[wb.SheetNames[0]];
const pdf = XLSX.utils.sheet_to_json(ws);

console.log(`PDF: ${pdf.length} profissionais`);

// Consulta a API local do resumo_dados_ssd para obter profissionais do banco
// Tenta porta padrão 8080
const options = {
  hostname: 'localhost',
  port: 8080,
  path: '/api/profissionais?size=500',
  method: 'GET',
  headers: { 'Accept': 'application/json' }
};

const req = http.request(options, (res) => {
  let body = '';
  res.on('data', chunk => body += chunk);
  res.on('end', () => {
    try {
      const json = JSON.parse(body);
      const profissionais = json.content || json || [];
      processar(profissionais);
    } catch (e) {
      console.error('Erro ao parsear resposta da API:', e.message);
      console.log('Resposta recebida:', body.substring(0, 500));
    }
  });
});

req.on('error', (e) => {
  console.error(`API local não acessível (${e.message})`);
  console.log('\nPara executar o matching, a aplicação precisa estar rodando.');
  console.log('Execute: mvnw spring-boot:run -Dspring-boot.run.profiles=dev');
  console.log('\n--- PRÉVIA DO MATCHING (sem banco, apenas PDF) ---');
  console.log('Campos disponíveis para cruzamento:');
  console.log('  PDF → Banco profissional : nome_normalizado');
  console.log('  PDF → Banco atendimento  : nome_normalizado + cbo_medico');
  console.log('\nNomes do PDF que serão usados no matching:');
  pdf.forEach((r, i) => {
    console.log(`  ${String(i+1).padStart(2,'0')}. ${norm(r.Nome).padEnd(45)} CBO: ${r.CBO}  CNS: ${r.CNS}`);
  });
});

req.end();

function processar(profissionais) {
  console.log(`\nBanco: ${profissionais.length} profissionais`);

  const matched = [];
  const unmatched = [];

  for (const p of pdf) {
    const nomeNorm = norm(p.Nome);

    // Tentativa 1: match exato por nome normalizado
    let found = profissionais.find(db => norm(db.nome) === nomeNorm);

    // Tentativa 2: match parcial (PDF pode ter nome truncado)
    if (!found) {
      found = profissionais.find(db => {
        const dbNorm = norm(db.nome);
        return dbNorm.startsWith(nomeNorm.substring(0, 20)) || nomeNorm.startsWith(dbNorm.substring(0, 20));
      });
    }

    if (found) {
      matched.push({
        pdf_nome: p.Nome,
        db_nome: found.nome,
        db_id: found.id,
        cns: p.CNS,
        cbo: p.CBO,
        match_type: norm(found.nome) === nomeNorm ? 'EXATO' : 'PARCIAL'
      });
    } else {
      unmatched.push({ pdf_nome: p.Nome, cns: p.CNS, cbo: p.CBO });
    }
  }

  console.log(`\nMatched  : ${matched.length}/${pdf.length}`);
  console.log(`Unmatched: ${unmatched.length}/${pdf.length}`);

  console.log('\n=== MATCHES ===');
  matched.forEach(m => {
    console.log(`[${m.match_type}] "${m.pdf_nome}" → id=${m.db_id} | CNS: ${m.cns}`);
  });

  if (unmatched.length > 0) {
    console.log('\n=== SEM MATCH ===');
    unmatched.forEach(u => console.log(`  "${u.pdf_nome}" CNS:${u.cns} CBO:${u.cbo}`));
  }

  // Gera SQL de UPDATE para popular o campo cns na tabela profissional
  console.log('\n=== SQL PARA POPULAR COLUNA CNS ===');
  matched.forEach(m => {
    console.log(`UPDATE profissional SET cns = '${m.cns}' WHERE id = ${m.db_id}; -- ${m.pdf_nome}`);
  });
}
