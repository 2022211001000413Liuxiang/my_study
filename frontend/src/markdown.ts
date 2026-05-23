const blockStart = (line = '', next = '') => /^```/.test(line)
  || /^(#{1,3})\s+/.test(line)
  || /^---+$/.test(line.trim())
  || /^\s*>\s?/.test(line)
  || /^\s*(-|\*)\s+/.test(line)
  || /^\s*\d+\.\s+/.test(line)
  || (line.includes('|') && /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(next));

export function renderMarkdown(markdown: string) {
  const lines = markdown.replace(/\r\n/g, '\n').split('\n');
  const blocks: string[] = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) {
      index += 1;
      continue;
    }

    const fence = /^```(\w+)?/.exec(line);
    if (fence) {
      const code: string[] = [];
      index += 1;
      while (index < lines.length && !/^```/.test(lines[index])) {
        code.push(lines[index]);
        index += 1;
      }
      index += 1;
      blocks.push(`<pre><code>${escapeHtml(code.join('\n'))}</code></pre>`);
      continue;
    }

    const heading = /^(#{1,3})\s+(.+)$/.exec(line);
    if (heading) {
      const level = heading[1].length;
      const text = heading[2].trim();
      blocks.push(`<h${level} id="${slugPlainText(text)}">${parseInline(text)}</h${level}>`);
      index += 1;
      continue;
    }

    if (/^---+$/.test(line.trim())) {
      blocks.push('<hr />');
      index += 1;
      continue;
    }

    if (line.includes('|') && index + 1 < lines.length && /^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(lines[index + 1])) {
      const tableLines = [line];
      index += 2;
      while (index < lines.length && lines[index].includes('|') && lines[index].trim()) {
        tableLines.push(lines[index]);
        index += 1;
      }
      blocks.push(renderTable(tableLines));
      continue;
    }

    if (/^\s*>\s?/.test(line)) {
      const quote: string[] = [];
      while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
        quote.push(lines[index].replace(/^\s*>\s?/, ''));
        index += 1;
      }
      blocks.push(`<blockquote>${parseInline(quote.join(' '))}</blockquote>`);
      continue;
    }

    if (/^\s*(-|\*)\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^\s*(-|\*)\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\s*(-|\*)\s+/, ''));
        index += 1;
      }
      blocks.push(`<ul>${items.map((item) => `<li>${parseInline(item)}</li>`).join('')}</ul>`);
      continue;
    }

    if (/^\s*\d+\.\s+/.test(line)) {
      const items: string[] = [];
      while (index < lines.length && /^\s*\d+\.\s+/.test(lines[index])) {
        items.push(lines[index].replace(/^\s*\d+\.\s+/, ''));
        index += 1;
      }
      blocks.push(`<ol>${items.map((item) => `<li>${parseInline(item)}</li>`).join('')}</ol>`);
      continue;
    }

    const paragraph = [line.trim()];
    index += 1;
    while (index < lines.length && lines[index].trim() && !blockStart(lines[index], lines[index + 1])) {
      paragraph.push(lines[index].trim());
      index += 1;
    }
    blocks.push(`<p>${parseInline(paragraph.join(' '))}</p>`);
  }

  return blocks.join('');
}

function renderTable(rows: string[]) {
  const parseRow = (row: string) => row.trim().replace(/^\||\|$/g, '').split('|').map((cell) => cell.trim());
  const [head, ...body] = rows.map(parseRow);
  return `<table><thead><tr>${head.map((cell) => `<th>${parseInline(cell)}</th>`).join('')}</tr></thead><tbody>${body.map((row) => `<tr>${row.map((cell) => `<td>${parseInline(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table>`;
}

function parseInline(text: string) {
  return text.split(/(!\[[^\]]*]\([^)]+\)|\[[^\]]+]\([^)]+\)|`[^`]+`|\*\*[^*]+\*\*)/g)
    .filter(Boolean)
    .map((part) => {
      const image = /^!\[([^\]]*)]\(([^)]+)\)$/.exec(part);
      if (image) return `<img alt="${escapeAttr(image[1])}" src="${escapeAttr(image[2])}" />`;
      const link = /^\[([^\]]+)]\(([^)]+)\)$/.exec(part);
      if (link) return `<a href="${escapeAttr(link[2])}" target="_blank" rel="noreferrer">${escapeHtml(link[1])}</a>`;
      if (part.startsWith('`') && part.endsWith('`')) return `<code>${escapeHtml(part.slice(1, -1))}</code>`;
      if (part.startsWith('**') && part.endsWith('**')) return `<strong>${escapeHtml(part.slice(2, -2))}</strong>`;
      return escapeHtml(part);
    })
    .join('');
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[char] || char));
}

function escapeAttr(value: string) {
  return escapeHtml(value).replace(/javascript:/gi, '');
}

function slugPlainText(text: string) {
  return text.replace(/[#*`]/g, '').trim().toLowerCase().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '');
}
