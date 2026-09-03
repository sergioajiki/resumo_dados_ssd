const fs = require('fs');
const XLSX = require('xlsx');

const VINCULACOES = [
  'VINCULO EMPREGATICIO CARGO COMISSIONAD',
  'VINCULO EMPREGATICIO ESTATUTARIO',
  'INTERMEDIADO',
  'INFORMAL',
  'ESTAGIO',
  'AUTONOMO',
];

const TIPOS = [
  'CONTRATADO TEMPORARIO',
  'VOLUNTARIAD O',
  'ESTAGIARIO',
  'PESSOA FISICA',
  'SERVIDOR PROPRIO',
  'SERVIDOR PUBLICO',
];

const SUBTIPOS = [
  'NAO SE APLICA',
  'PROPRIO',
];

function normalize(s) {
  return s.replace(/\s+/g, ' ').trim();
}

function extractField(text, patterns) {
  for (const p of patterns) {
    if (text.startsWith(p)) {
      return [p, text.substring(p.length).trim()];
    }
  }
  return ['', text];
}

function cleanTipo(s) {
  return s === 'VOLUNTARIAD O' ? 'VOLUNTARIADO' : s;
}

function cleanVinculacao(s) {
  return s === 'VINCULO EMPREGATICIO CARGO COMISSIONAD'
    ? 'VINCULO EMPREGATICIO CARGO COMISSIONADO'
    : s;
}

const rawText = fs.readFileSync('pdf_raw_text.txt', 'utf8');

// Remove page headers and footers
let text = rawText
  .replace(/--- PAGE \d+ ---/g, ' ')
  .replace(/Nome\s+CNS\s+CBO[\s\S]*?CHS Total/g, ' ')
  .replace(/\d+ Total de profissionais[\s\S]*?de 4/g, ' ')
  .replace(/Estabelecimento de Saúde[\s\S]*?CNPJ Mantenedora:/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

const records = [];
// Split by 15-digit CNS numbers
const cnsRegex = /(\d{15})\s+/g;
const parts = [];
let lastIndex = 0;
let m;

while ((m = cnsRegex.exec(text)) !== null) {
  if (lastIndex > 0 || parts.length > 0) {
    // Previous segment ends here
    if (parts.length > 0) {
      parts[parts.length - 1].end = m.index;
    }
  }
  parts.push({ cns: m[1], start: m.index + m[0].length, end: text.length });
  lastIndex = m.index;
}

for (const part of parts) {
  const cns = part.cns;
  const rest = normalize(text.substring(part.start, part.end));

  // Find CBO (6-digit number followed by " - ")
  const cboMatch = rest.match(/^(.*?)\s+(\d{6})\s+-\s+([\s\S]*)/);
  if (!cboMatch) {
    console.warn('No CBO found for CNS', cns, '| text:', rest.substring(0, 80));
    continue;
  }

  const nome = normalize(cboMatch[1]);
  const cbo = cboMatch[2];
  let afterCbo = normalize(cboMatch[3]);

  // Find SIM (SUS field)
  const simIdx = afterCbo.indexOf(' SIM ');
  if (simIdx === -1) {
    console.warn('No SIM found for CNS', cns);
    continue;
  }

  const especialidade = normalize(afterCbo.substring(0, simIdx));
  let afterSim = normalize(afterCbo.substring(simIdx + 5));

  const sus = 'SIM';

  let [vinculacao, rem1] = extractField(afterSim, VINCULACOES);
  let [tipo, rem2] = extractField(rem1, TIPOS);
  let [subtipo] = extractField(rem2, SUBTIPOS);

  records.push({
    Nome: nome,
    CNS: cns,
    CBO: cbo,
    Especialidade: especialidade,
    SUS: sus,
    Vinculação: cleanVinculacao(vinculacao),
    Tipo: cleanTipo(tipo),
    Subtipo: subtipo,
  });
}

console.log(`Extracted ${records.length} records`);
records.forEach(r => console.log(JSON.stringify(r)));

// Export to Excel
const wb = XLSX.utils.book_new();
const ws = XLSX.utils.json_to_sheet(records);

// Auto column widths
const cols = Object.keys(records[0] || {});
ws['!cols'] = cols.map(col => ({
  wch: Math.max(col.length, ...records.map(r => String(r[col] || '').length))
}));

XLSX.utils.book_append_sheet(wb, ws, 'Profissionais');

const outputPath = 'C:/Users/sergioa/Downloads/Prof_NTSMS_6970451.xlsx';
XLSX.writeFile(wb, outputPath);
console.log(`\nExported to: ${outputPath}`);
