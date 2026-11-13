import React, { useState } from 'react';
import { Copy, Check } from 'lucide-react';

interface MarkdownViewProps {
  content: string;
  isDarkMode?: boolean;
}

export function MarkdownView({ content, isDarkMode = true }: MarkdownViewProps) {
  // Parse content into code blocks and normal markdown segments
  const parts = parseMarkdown(content);

  return (
    <div className="space-y-2 text-sm leading-relaxed overflow-hidden">
      {parts.map((part, index) => {
        if (part.type === 'code') {
          return (
            <CodeBlock 
              key={index} 
              code={part.code} 
              language={part.language} 
              isDarkMode={isDarkMode} 
            />
          );
        }
        return (
          <FormattedText key={index} text={part.text} isDarkMode={isDarkMode} />
        );
      })}
    </div>
  );
}

function CodeBlock({ 
  code, 
  language, 
  isDarkMode 
}: { 
  code: string; 
  language: string; 
  isDarkMode: boolean; 
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={`my-2.5 rounded-2xl border overflow-hidden text-xs font-mono shadow-sm ${
      isDarkMode 
        ? 'bg-[#090b0e] border-white/10 text-neutral-200' 
        : 'bg-[#181a1f] border-neutral-700 text-neutral-100'
    }`}>
      {/* Code Header Bar */}
      <div className={`px-4 py-1.5 flex items-center justify-between border-b ${
        isDarkMode ? 'bg-white/[0.04] border-white/10' : 'bg-neutral-800 border-neutral-700'
      }`}>
        <span className="text-[11px] uppercase tracking-wider text-neutral-400 font-semibold">
          {language || 'code'}
        </span>
        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] text-neutral-300 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
          title="Copy code to clipboard"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              <span className="text-emerald-400 font-medium">Copied</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>

      {/* Code Content */}
      <pre className="p-4 overflow-x-auto text-[13px] leading-relaxed select-text">
        <code>{code}</code>
      </pre>
    </div>
  );
}

function FormattedText({ text, isDarkMode }: { text: string; isDarkMode: boolean }) {
  const lines = text.split('\n');

  return (
    <div className="space-y-1.5">
      {lines.map((line, idx) => {
        const trimmed = line.trim();
        if (!trimmed) return <div key={idx} className="h-1" />;

        // Headers
        if (trimmed.startsWith('### ')) {
          return (
            <h4 key={idx} className="font-bold text-sm tracking-tight pt-1">
              {formatInline(trimmed.slice(4), isDarkMode)}
            </h4>
          );
        }
        if (trimmed.startsWith('## ')) {
          return (
            <h3 key={idx} className="font-bold text-base tracking-tight pt-1.5">
              {formatInline(trimmed.slice(3), isDarkMode)}
            </h3>
          );
        }
        if (trimmed.startsWith('# ')) {
          return (
            <h2 key={idx} className="font-bold text-lg tracking-tight pt-2">
              {formatInline(trimmed.slice(2), isDarkMode)}
            </h2>
          );
        }

        // Bullet lists
        if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
          return (
            <div key={idx} className="flex items-start gap-2 pl-2">
              <span className="opacity-50 mt-1 select-none">•</span>
              <span className="flex-1">{formatInline(trimmed.slice(2), isDarkMode)}</span>
            </div>
          );
        }

        // Numbered lists
        const numberedMatch = trimmed.match(/^(\d+)\.\s+(.*)/);
        if (numberedMatch) {
          return (
            <div key={idx} className="flex items-start gap-2 pl-2">
              <span className="font-mono text-xs opacity-60 mt-0.5 select-none">{numberedMatch[1]}.</span>
              <span className="flex-1">{formatInline(numberedMatch[2], isDarkMode)}</span>
            </div>
          );
        }

        return <p key={idx}>{formatInline(line, isDarkMode)}</p>;
      })}
    </div>
  );
}

// Inline formatting: `code`, **bold**, *italic*
function formatInline(str: string, isDarkMode: boolean): React.ReactNode[] {
  // Regex splitting by backticks, bold, italic
  const tokens = str.split(/(`[^`]+`|\*\*[^*]+\*\*|\*[^*]+\*)/g);

  return tokens.map((token, i) => {
    if (token.startsWith('`') && token.endsWith('`') && token.length >= 2) {
      return (
        <code 
          key={i} 
          className={`px-1.5 py-0.5 rounded text-xs font-mono font-medium ${
            isDarkMode ? 'bg-white/10 text-amber-300' : 'bg-neutral-200 text-amber-800'
          }`}
        >
          {token.slice(1, -1)}
        </code>
      );
    }
    if (token.startsWith('**') && token.endsWith('**') && token.length >= 4) {
      return <strong key={i} className="font-semibold">{token.slice(2, -2)}</strong>;
    }
    if (token.startsWith('*') && token.endsWith('*') && token.length >= 2) {
      return <em key={i} className="italic">{token.slice(1, -1)}</em>;
    }
    return token;
  });
}

function parseMarkdown(content: string): Array<{ type: 'code'; code: string; language: string } | { type: 'text'; text: string }> {
  const parts: Array<{ type: 'code'; code: string; language: string } | { type: 'text'; text: string }> = [];
  const codeRegex = /```(\w*)\n([\s\S]*?)```/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = codeRegex.hasIndices ? codeRegex.exec(content) : codeRegex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push({
        type: 'text',
        text: content.slice(lastIndex, match.index)
      });
    }
    parts.push({
      type: 'code',
      language: match[1] || '',
      code: match[2].trimEnd()
    });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < content.length) {
    parts.push({
      type: 'text',
      text: content.slice(lastIndex)
    });
  }

  return parts;
}
