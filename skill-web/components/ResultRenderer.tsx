// ============================================
// 结果渲染组件
// ============================================
// 负责将技能执行返回的 JSON 数据呈现在页面上
// ============================================

import React, { useState } from 'react';
import { FileJson, ChevronDown, ChevronRight, Copy, Check } from 'lucide-react';

interface ResultRendererProps {
  output: any;
}

/**
 * JSON 值渲染 — 根据类型显示不同样式
 */
const JsonValue: React.FC<{ value: any }> = ({ value }) => {
  if (value === null) return <span className="text-slate-400 italic">null</span>;
  if (value === undefined) return <span className="text-slate-400 italic">undefined</span>;
  if (typeof value === 'boolean') {
    return (
      <span className={`font-mono text-sm ${value ? 'text-emerald-600' : 'text-red-500'}`}>
        {String(value)}
      </span>
    );
  }
  if (typeof value === 'number') {
    return <span className="font-mono text-sm text-indigo-600">{value}</span>;
  }
  if (typeof value === 'string') {
    if (value.startsWith('http://') || value.startsWith('https://')) {
      return (
        <a href={value} target="_blank" rel="noopener noreferrer"
           className="text-sm text-indigo-600 hover:underline break-all">
          {value}
        </a>
      );
    }
    // 长文本折行显示
    return <span className="text-sm text-slate-700 break-words whitespace-pre-wrap">{value}</span>;
  }
  return <span className="text-sm text-slate-700">{String(value)}</span>;
};

/**
 * 可折叠的 JSON 节点
 */
const JsonNode: React.FC<{ label: string; data: any; defaultOpen?: boolean }> = ({ label, data, defaultOpen = true }) => {
  const [open, setOpen] = useState(defaultOpen);
  const isArray = Array.isArray(data);
  const isObject = typeof data === 'object' && data !== null;
  const isPrimitive = !isObject;

  if (isPrimitive) {
    return (
      <div className="flex items-baseline py-1.5 px-3 hover:bg-slate-50 rounded">
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide min-w-[120px] shrink-0 mr-3">
          {formatKey(label)}
        </span>
        <JsonValue value={data} />
      </div>
    );
  }

  const entries = isArray ? data.map((v: any, i: number) => [String(i), v]) : Object.entries(data);
  const summary = isArray ? `[ ${data.length} items ]` : `{ ${Object.keys(data).length} fields }`;

  return (
    <div className="border-l-2 border-slate-100 ml-1">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center w-full text-left py-1.5 px-3 hover:bg-slate-50 rounded transition-colors group"
      >
        {open
          ? <ChevronDown size={14} className="text-slate-400 mr-1.5 shrink-0" />
          : <ChevronRight size={14} className="text-slate-400 mr-1.5 shrink-0" />
        }
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide mr-2">
          {formatKey(label)}
        </span>
        {!open && <span className="text-xs text-slate-400 font-mono">{summary}</span>}
      </button>
      {open && (
        <div className="ml-4">
          {entries.map(([key, value]: [string, any]) => (
            <JsonNode
              key={key}
              label={isArray ? `#${key}` : key}
              data={value}
              defaultOpen={false}
            />
          ))}
        </div>
      )}
    </div>
  );
};

/**
 * 格式化 key：snake_case / camelCase → 可读形式
 */
function formatKey(key: string): string {
  return key
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, s => s.toUpperCase())
    .trim();
}

/**
 * 复制按钮
 */
const CopyButton: React.FC<{ text: string }> = ({ text }) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <button
      onClick={handleCopy}
      className="flex items-center text-xs text-slate-400 hover:text-indigo-600 transition-colors px-2 py-1 rounded hover:bg-slate-100"
      title="Copy JSON"
    >
      {copied
        ? <><Check size={12} className="mr-1 text-emerald-500" /><span className="text-emerald-500">Copied</span></>
        : <><Copy size={12} className="mr-1" />Copy JSON</>
      }
    </button>
  );
};

/**
 * 主渲染组件
 *
 * Skill 执行返回 JSON 数据，本组件将其以结构化方式呈现。
 * - 对象/数组: 可折叠的树形结构
 * - 原始值: 带类型样式的内联显示
 */
export const ResultRenderer: React.FC<ResultRendererProps> = ({ output }) => {
  if (output === null || output === undefined) return null;
  const [showRaw, setShowRaw] = useState(false);

  const jsonText = JSON.stringify(output, null, 2);
  const isObject = typeof output === 'object';

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden animate-fade-in">
      {/* Header */}
      <div className="bg-slate-50 px-4 py-3 border-b border-slate-100 flex justify-between items-center">
        <div className="flex items-center space-x-2">
          <FileJson size={16} className="text-indigo-500" />
          <span className="font-semibold text-slate-700 text-sm">Execution Result</span>
        </div>
        <div className="flex items-center space-x-2">
          {isObject && (
            <button
              onClick={() => setShowRaw(!showRaw)}
              className="text-xs text-slate-400 hover:text-indigo-600 transition-colors px-2 py-1 rounded hover:bg-slate-100"
            >
              {showRaw ? 'Tree View' : 'Raw JSON'}
            </button>
          )}
          <CopyButton text={jsonText} />
        </div>
      </div>

      {/* Content */}
      <div className="p-4 md:p-6">
        {showRaw || !isObject ? (
          // Raw JSON 或原始值
          <pre className="text-sm font-mono text-slate-700 bg-slate-50 p-4 rounded-lg overflow-x-auto whitespace-pre-wrap break-words max-h-[600px] overflow-y-auto">
            {jsonText}
          </pre>
        ) : (
          // 结构化树形视图
          <div className="space-y-0.5">
            {Array.isArray(output)
              ? output.map((item, i) => (
                  <JsonNode key={i} label={`#${i}`} data={item} defaultOpen={i < 5} />
                ))
              : Object.entries(output).map(([key, value]) => (
                  <JsonNode key={key} label={key} data={value} defaultOpen={true} />
                ))
            }
          </div>
        )}
      </div>
    </div>
  );
};
