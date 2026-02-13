// ============================================
// Aegis Skill Web - 主应用组件
// ============================================
// 本文件是前端应用的根组件，负责：
// 1. 展示技能/工具列表侧边栏（Tab 切换）
// 2. 处理技能选择和搜索
// 3. 管理技能执行状态
// 4. 渲染动态表单和执行结果
// 5. 展示工具详情（inputSchema / outputSchema / errorDescriptions）
// ============================================

import React, { useState, useEffect } from 'react';
import { Activity, Search, Box, Wrench, ChevronRight, PlayCircle, Clock, AlertTriangle, RotateCcw, ShieldCheck, CheckCircle, XCircle, Info, Loader2, FileText, Send } from 'lucide-react';
import { Skill, ToolInfo, ExecutionState, SkillValidationReport, ValidationIssue } from './types';
import { skillService } from './services/skillService';
import { DynamicForm } from './components/DynamicForm';
import { ResultRenderer } from './components/ResultRenderer';

type ActiveTab = 'skills' | 'tools' | 'validate';

const App: React.FC = () => {
  // Tab state
  const [activeTab, setActiveTab] = useState<ActiveTab>('skills');

  // Skills state
  const [skills, setSkills] = useState<Skill[]>([]);
  const [selectedSkill, setSelectedSkill] = useState<Skill | null>(null);

  // Tools state
  const [tools, setTools] = useState<ToolInfo[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedTool, setSelectedTool] = useState<ToolInfo | null>(null);

  // Validation state
  const [validationReports, setValidationReports] = useState<SkillValidationReport[]>([]);
  const [selectedReport, setSelectedReport] = useState<SkillValidationReport | null>(null);
  const [validationLoading, setValidationLoading] = useState(false);

  // Markdown validation state
  const [showMarkdownValidator, setShowMarkdownValidator] = useState(false);
  const [markdownInput, setMarkdownInput] = useState('');
  const [markdownValidating, setMarkdownValidating] = useState(false);
  const [markdownReport, setMarkdownReport] = useState<SkillValidationReport | null>(null);
  const [markdownError, setMarkdownError] = useState<string | null>(null);

  // Shared
  const [searchQuery, setSearchQuery] = useState('');

  // Execution State
  const [execState, setExecState] = useState<ExecutionState>({
    status: 'IDLE',
    loading: false
  });

  // Load data on mount
  useEffect(() => {
    skillService.getSkills()
      .then(setSkills)
      .catch(err => console.error("Failed to load skills", err));
    skillService.getTools()
      .then(setTools)
      .catch(err => console.error("Failed to load tools", err));
    skillService.getToolCategories()
      .then(setCategories)
      .catch(err => console.error("Failed to load tool categories", err));
  }, []);

  // Filter Logic — Skills
  const filteredSkills = skills.filter(s =>
    s.id.includes(searchQuery.toLowerCase()) ||
    s.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
    s.intents.some(i => i.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  // Filter Logic — Tools
  const filteredTools = tools.filter(t => {
    const q = searchQuery.toLowerCase();
    const matchesSearch = !q ||
      t.name.toLowerCase().includes(q) ||
      (t.description && t.description.toLowerCase().includes(q)) ||
      (t.category && t.category.toLowerCase().includes(q)) ||
      (t.tags && t.tags.some(tag => tag.toLowerCase().includes(q)));
    const matchesCategory = !selectedCategory || t.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const handleTabChange = (tab: ActiveTab) => {
    setActiveTab(tab);
    setSearchQuery('');
    if (tab === 'validate' && validationReports.length === 0) {
      loadValidationReports();
    }
  };

  const loadValidationReports = () => {
    setValidationLoading(true);
    skillService.validateAllSkills()
      .then(reports => {
        setValidationReports(reports);
        setValidationLoading(false);
      })
      .catch(err => {
        console.error("Failed to load validation reports", err);
        setValidationLoading(false);
      });
  };

  const handleSelectSkill = (skill: Skill) => {
    setSelectedSkill(skill);
    setSelectedTool(null);
    setSelectedReport(null);
    setExecState({ status: 'IDLE', loading: false });
  };

  const handleSelectTool = (tool: ToolInfo) => {
    setSelectedTool(tool);
    setSelectedSkill(null);
    setSelectedReport(null);
    setExecState({ status: 'IDLE', loading: false });
  };

  const handleSelectReport = (report: SkillValidationReport) => {
    setSelectedReport(report);
    setSelectedSkill(null);
    setSelectedTool(null);
    setShowMarkdownValidator(false);
  };

  const handleOpenMarkdownValidator = () => {
    setShowMarkdownValidator(true);
    setSelectedReport(null);
    setSelectedSkill(null);
    setSelectedTool(null);
    setMarkdownReport(null);
    setMarkdownError(null);
  };

  const handleValidateMarkdown = async () => {
    if (!markdownInput.trim()) return;
    setMarkdownValidating(true);
    setMarkdownError(null);
    try {
      const report = await skillService.validateMarkdown(markdownInput);
      setMarkdownReport(report);
    } catch (err: any) {
      setMarkdownError(err.message || 'Validation request failed');
    } finally {
      setMarkdownValidating(false);
    }
  };

  // Filter Logic — Validation Reports
  const filteredReports = validationReports.filter(r => {
    if (!r.summary) return false;
    const q = searchQuery.toLowerCase();
    return !q || r.summary.skillId.toLowerCase().includes(q) ||
      (r.summary.description && r.summary.description.toLowerCase().includes(q));
  });

  const handleInitialSubmit = async (data: any) => {
    if (!selectedSkill) return;

    setExecState({ status: 'IDLE', loading: true });

    try {
      const response = await skillService.executeSkill(selectedSkill.id, data, selectedSkill.version);

      setExecState({
        status: response.status,
        loading: false,
        executionId: response.executionId,
        awaitMessage: response.awaitMessage,
        currentSchema: response.awaitSchema ? { fields: response.awaitSchema } : undefined,
        result: response.output,
        error: response.error
      });
    } catch (err: any) {
      setExecState({
        status: 'FAILED',
        loading: false,
        error: err.message || "Network or Server Error"
      });
    }
  };

  const handleResumeSubmit = async (data: any) => {
    if (!selectedSkill || !execState.executionId) return;

    setExecState(prev => ({ ...prev, loading: true }));

    try {
      const response = await skillService.resumeExecution(execState.executionId, selectedSkill.id, data, selectedSkill.version);

      setExecState({
        status: response.status,
        loading: false,
        executionId: response.executionId,
        awaitMessage: response.awaitMessage,
        currentSchema: response.awaitSchema ? { fields: response.awaitSchema } : undefined,
        result: response.output,
        error: response.error
      });

    } catch (err: any) {
        setExecState({
            status: 'FAILED',
            loading: false,
            error: err.message || "Failed to resume execution"
        });
    }
  };

  const resetExecution = () => {
      setExecState({ status: 'IDLE', loading: false });
  };

  // Helper: extract schema properties as a flat array for table rendering
  const extractSchemaFields = (schema: Record<string, any> | undefined): { name: string; type: string; required: boolean; description: string }[] => {
    if (!schema) return [];
    const properties = schema.properties || schema;
    const requiredList: string[] = schema.required || [];
    return Object.entries(properties).map(([name, spec]: [string, any]) => ({
      name,
      type: spec?.type || 'unknown',
      required: requiredList.includes(name) || !!spec?.required,
      description: spec?.description || ''
    }));
  };

  const hasSelection = selectedSkill || selectedTool || selectedReport || showMarkdownValidator;

  return (
    <div className="flex h-screen w-full bg-slate-50 overflow-hidden text-slate-900">

      {/* Sidebar */}
      <div className="w-80 md:w-96 flex flex-col border-r border-slate-200 bg-white shadow-sm z-10">
        <div className="p-4 border-b border-slate-100">
          <div className="flex items-center space-x-2 mb-4">
             <div className="bg-indigo-600 p-2 rounded-lg">
                <Box className="text-white" size={20} />
             </div>
             <h1 className="font-bold text-xl tracking-tight text-slate-800">Skill Engine</h1>
          </div>

          {/* Tab Buttons */}
          <div className="flex mb-3 bg-slate-100 rounded-lg p-1">
            <button
              onClick={() => handleTabChange('skills')}
              className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                activeTab === 'skills'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <Box size={14} />
              Skills
            </button>
            <button
              onClick={() => handleTabChange('tools')}
              className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                activeTab === 'tools'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <Wrench size={14} />
              Tools
            </button>
            <button
              onClick={() => handleTabChange('validate')}
              className={`flex-1 flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                activeTab === 'validate'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <ShieldCheck size={14} />
              Validate
            </button>
          </div>

          {/* Search */}
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input
              type="text"
              placeholder={activeTab === 'skills' ? 'Search skills...' : activeTab === 'tools' ? 'Search tools...' : 'Search validation reports...'}
              className="w-full pl-10 pr-4 py-2 bg-slate-100 border-none rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {/* Category Filter — Tools only */}
          {activeTab === 'tools' && categories.length > 0 && (
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="w-full mt-2 px-3 py-2 bg-slate-100 border-none rounded-md text-sm focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
            >
              <option value="">All Categories</option>
              {categories.map(cat => (
                <option key={cat} value={cat}>{cat}</option>
              ))}
            </select>
          )}
        </div>

        {/* List Area */}
        <div className="flex-1 overflow-y-auto p-3 space-y-2">

          {/* Skills List */}
          {activeTab === 'skills' && (
            <>
              {skills.length === 0 && (
                <div className="text-center text-slate-400 py-10 text-sm">
                   Loading skills...
                </div>
              )}
              {filteredSkills.map(skill => (
                <button
                  key={`${skill.id}@${skill.version || ''}`}
                  onClick={() => handleSelectSkill(skill)}
                  className={`w-full text-left p-3 rounded-lg border transition-all duration-200 group ${
                    selectedSkill?.id === skill.id && selectedSkill?.version === skill.version
                    ? 'bg-indigo-50 border-indigo-200 shadow-sm'
                    : 'bg-white border-transparent hover:border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                      <div className="flex items-center gap-2">
                          <h3 className={`font-semibold text-sm ${selectedSkill?.id === skill.id && selectedSkill?.version === skill.version ? 'text-indigo-700' : 'text-slate-700'}`}>
                              {skill.id}
                          </h3>
                          {skill.version && (
                              <span className="text-[10px] font-mono px-1.5 py-0.5 bg-indigo-50 text-indigo-500 rounded border border-indigo-100">
                                  v{skill.version}
                              </span>
                          )}
                      </div>
                      {selectedSkill?.id === skill.id && selectedSkill?.version === skill.version && <ChevronRight size={16} className="text-indigo-500" />}
                  </div>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">{skill.description}</p>
                  <div className="flex flex-wrap gap-1 mt-2">
                      {skill.intents.slice(0, 2).map(intent => (
                          <span key={intent} className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                              {intent}
                          </span>
                      ))}
                  </div>
                </button>
              ))}
            </>
          )}

          {/* Tools List */}
          {activeTab === 'tools' && (
            <>
              {tools.length === 0 && (
                <div className="text-center text-slate-400 py-10 text-sm">
                   Loading tools...
                </div>
              )}
              {filteredTools.map(tool => (
                <button
                  key={tool.name}
                  onClick={() => handleSelectTool(tool)}
                  className={`w-full text-left p-3 rounded-lg border transition-all duration-200 group ${
                    selectedTool?.name === tool.name
                    ? 'bg-indigo-50 border-indigo-200 shadow-sm'
                    : 'bg-white border-transparent hover:border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex items-center gap-2">
                      <h3 className={`font-semibold text-sm ${selectedTool?.name === tool.name ? 'text-indigo-700' : 'text-slate-700'}`}>
                        {tool.name}
                      </h3>
                    </div>
                    {selectedTool?.name === tool.name && <ChevronRight size={16} className="text-indigo-500" />}
                  </div>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">{tool.description}</p>
                  <div className="flex flex-wrap gap-1 mt-2">
                    {tool.category && (
                      <span className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-indigo-50 text-indigo-600 rounded">
                        {tool.category}
                      </span>
                    )}
                    {tool.tags?.slice(0, 2).map(tag => (
                      <span key={tag} className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                        {tag}
                      </span>
                    ))}
                  </div>
                </button>
              ))}
            </>
          )}

          {/* Validation Reports List */}
          {activeTab === 'validate' && (
            <>
              {/* Validate Markdown Entry */}
              <button
                onClick={handleOpenMarkdownValidator}
                className={`w-full text-left p-3 rounded-lg border transition-all duration-200 mb-2 ${
                  showMarkdownValidator
                    ? 'bg-indigo-50 border-indigo-200 shadow-sm'
                    : 'bg-white border-dashed border-slate-300 hover:border-indigo-300 hover:bg-indigo-50/50'
                }`}
              >
                <div className="flex items-center gap-2">
                  <FileText size={16} className={showMarkdownValidator ? 'text-indigo-600' : 'text-slate-400'} />
                  <span className={`text-sm font-medium ${showMarkdownValidator ? 'text-indigo-700' : 'text-slate-600'}`}>
                    Validate Markdown
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-1 ml-6">Paste or edit skill markdown content to validate</p>
              </button>

              {validationLoading && (
                <div className="text-center text-slate-400 py-10 text-sm flex flex-col items-center gap-2">
                  <Loader2 size={20} className="animate-spin" />
                  Validating skills...
                </div>
              )}
              {!validationLoading && validationReports.length === 0 && (
                <div className="text-center text-slate-400 py-10 text-sm">
                  No validation reports available.
                </div>
              )}
              {!validationLoading && filteredReports.map(report => {
                const sid = report.summary?.skillId || 'unknown';
                const isSelected = selectedReport?.summary?.skillId === sid
                  && selectedReport?.summary?.version === report.summary?.version;
                return (
                  <button
                    key={`${sid}@${report.summary?.version || ''}`}
                    onClick={() => handleSelectReport(report)}
                    className={`w-full text-left p-3 rounded-lg border transition-all duration-200 group ${
                      isSelected
                      ? 'bg-indigo-50 border-indigo-200 shadow-sm'
                      : 'bg-white border-transparent hover:border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex justify-between items-start">
                      <div className="flex items-center gap-2">
                        {report.valid
                          ? <CheckCircle size={14} className="text-emerald-500" />
                          : <XCircle size={14} className="text-red-500" />
                        }
                        <h3 className={`font-semibold text-sm ${isSelected ? 'text-indigo-700' : 'text-slate-700'}`}>
                          {sid}
                        </h3>
                        {report.summary?.version && (
                          <span className="text-[10px] font-mono px-1.5 py-0.5 bg-indigo-50 text-indigo-500 rounded border border-indigo-100">
                            v{report.summary.version}
                          </span>
                        )}
                      </div>
                      {isSelected && <ChevronRight size={16} className="text-indigo-500" />}
                    </div>
                    <p className="text-xs text-slate-500 mt-1 line-clamp-2">
                      {report.summary?.description || 'No description'}
                    </p>
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {report.errorCount > 0 && (
                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-red-50 text-red-600 rounded border border-red-100">
                          {report.errorCount} error{report.errorCount > 1 ? 's' : ''}
                        </span>
                      )}
                      {report.warningCount > 0 && (
                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-amber-50 text-amber-600 rounded border border-amber-100">
                          {report.warningCount} warning{report.warningCount > 1 ? 's' : ''}
                        </span>
                      )}
                      {report.suggestionCount > 0 && (
                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded border border-blue-100">
                          {report.suggestionCount} suggestion{report.suggestionCount > 1 ? 's' : ''}
                        </span>
                      )}
                      {report.valid && report.errorCount === 0 && report.warningCount === 0 && report.suggestionCount === 0 && (
                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-emerald-50 text-emerald-600 rounded border border-emerald-100">
                          All checks passed
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </>
          )}
        </div>

        <div className="p-3 border-t border-slate-100 text-center">
            <span className="text-xs text-slate-400">Aegis Frontend Engine v1.0.0</span>
        </div>
      </div>

      {/* Main Workspace */}
      <div className="flex-1 flex flex-col h-full overflow-hidden relative">

         {/* Empty State */}
         {!hasSelection && (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-300">
                <Activity size={64} className="mb-4 text-slate-200" />
                <p className="text-lg font-medium text-slate-400">Select a skill or tool to view details</p>
            </div>
         )}

         {/* Skill Execution View */}
         {selectedSkill && (
            <>
                {/* Header */}
                <header className="h-16 px-8 flex items-center justify-between bg-white border-b border-slate-200 shrink-0">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 flex items-center">
                            {selectedSkill.id}
                            {selectedSkill.version && (
                                <span className="ml-2 px-2 py-0.5 rounded text-xs font-mono font-normal bg-indigo-50 text-indigo-600 border border-indigo-100">
                                    v{selectedSkill.version}
                                </span>
                            )}
                            <span className="ml-2 px-2 py-0.5 rounded text-xs font-normal bg-slate-100 text-slate-500 border border-slate-200">
                                {selectedSkill.intents[0]}
                            </span>
                        </h2>
                        <p className="text-xs text-slate-500">{selectedSkill.description}</p>
                    </div>

                    {execState.status !== 'IDLE' && (
                        <button
                            onClick={resetExecution}
                            className="text-xs flex items-center text-slate-500 hover:text-indigo-600 transition-colors"
                        >
                            <RotateCcw size={14} className="mr-1" />
                            Reset Execution
                        </button>
                    )}
                </header>

                {/* Scrollable Content Area */}
                <main className="flex-1 overflow-y-auto p-4 md:p-8">
                    <div className="max-w-3xl mx-auto pb-20">

                        {/* IDLE: Initial Form */}
                        {execState.status === 'IDLE' && (
                            <div className="bg-white p-6 md:p-8 rounded-xl border border-slate-200 shadow-sm animate-fade-in">
                                <div className="flex items-center space-x-3 mb-6 pb-4 border-b border-slate-100">
                                    <div className="p-2 bg-indigo-100 rounded-lg text-indigo-600">
                                        <PlayCircle size={24} />
                                    </div>
                                    <div>
                                        <h3 className="font-semibold text-lg text-slate-800">Configure Execution</h3>
                                        <p className="text-sm text-slate-500">Provide initial parameters to start the skill.</p>
                                    </div>
                                </div>
                                <DynamicForm
                                    schema={selectedSkill.inputSchema}
                                    onSubmit={handleInitialSubmit}
                                    isLoading={execState.loading}
                                    submitLabel="Start Execution"
                                />
                            </div>
                        )}

                        {/* WAITING_FOR_INPUT: Resume Form */}
                        {execState.status === 'WAITING_FOR_INPUT' && execState.currentSchema && (
                            <div className="bg-amber-50 p-6 md:p-8 rounded-xl border border-amber-200 shadow-sm animate-fade-in relative overflow-hidden">
                                <div className="absolute top-0 right-0 p-4 opacity-10">
                                    <Clock size={120} />
                                </div>
                                <div className="relative z-10">
                                    <div className="flex items-center space-x-3 mb-6">
                                        <div className="p-2 bg-amber-200 rounded-lg text-amber-700 animate-pulse">
                                            <Clock size={24} />
                                        </div>
                                        <div>
                                            <h3 className="font-semibold text-lg text-amber-900">Action Required</h3>
                                            <p className="text-sm text-amber-700 font-medium">{execState.awaitMessage}</p>
                                        </div>
                                    </div>

                                    <div className="bg-white/60 p-6 rounded-lg backdrop-blur-sm">
                                        <DynamicForm
                                            schema={execState.currentSchema}
                                            onSubmit={handleResumeSubmit}
                                            isLoading={execState.loading}
                                            submitLabel="Resume Execution"
                                        />
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* COMPLETED: Result */}
                        {execState.status === 'COMPLETED' && (
                            <div className="animate-slide-up">
                                <div className="flex items-center space-x-2 mb-4 text-emerald-600 font-medium px-2">
                                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                                    <span>Execution Successful</span>
                                </div>
                                <ResultRenderer output={execState.result} />
                            </div>
                        )}

                        {/* FAILED: Error */}
                        {execState.status === 'FAILED' && (
                            <div className="bg-red-50 p-6 rounded-xl border border-red-200 flex items-start space-x-4 animate-shake">
                                <AlertTriangle className="text-red-500 mt-1 shrink-0" size={24} />
                                <div>
                                    <h3 className="font-semibold text-red-800">Execution Failed</h3>
                                    <p className="text-red-600 text-sm mt-1">{execState.error || 'An unexpected error occurred.'}</p>
                                    <button
                                        onClick={resetExecution}
                                        className="mt-4 px-4 py-2 bg-white border border-red-200 text-red-600 text-sm font-medium rounded hover:bg-red-100 transition-colors"
                                    >
                                        Try Again
                                    </button>
                                </div>
                            </div>
                        )}

                    </div>
                </main>
            </>
         )}

         {/* Tool Detail View */}
         {selectedTool && (
            <>
                {/* Header */}
                <header className="h-16 px-8 flex items-center bg-white border-b border-slate-200 shrink-0">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 flex items-center">
                            <Wrench size={18} className="mr-2 text-indigo-500" />
                            {selectedTool.name}
                            {selectedTool.category && (
                                <span className="ml-2 px-2 py-0.5 rounded text-xs font-normal bg-slate-100 text-slate-500 border border-slate-200">
                                    {selectedTool.category}
                                </span>
                            )}
                        </h2>
                        <p className="text-xs text-slate-500">{selectedTool.description}</p>
                    </div>
                </header>

                {/* Tool Detail Content */}
                <main className="flex-1 overflow-y-auto p-4 md:p-8">
                    <div className="max-w-3xl mx-auto pb-20 space-y-6">

                        {/* Tags */}
                        {selectedTool.tags && selectedTool.tags.length > 0 && (
                            <div className="flex flex-wrap gap-2">
                                {selectedTool.tags.map(tag => (
                                    <span key={tag} className="text-xs font-medium px-2 py-1 bg-slate-100 text-slate-600 rounded">
                                        {tag}
                                    </span>
                                ))}
                            </div>
                        )}

                        {/* Input Schema */}
                        {selectedTool.inputSchema && Object.keys(selectedTool.inputSchema).length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-slate-50">
                                    <h3 className="font-semibold text-slate-800">Input Schema</h3>
                                </div>
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-slate-100">
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Parameter</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Type</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Required</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Description</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {extractSchemaFields(selectedTool.inputSchema).map((field) => (
                                                <tr key={field.name} className="border-b border-slate-50 hover:bg-slate-50">
                                                    <td className="px-6 py-3 font-mono text-sm text-indigo-600">{field.name}</td>
                                                    <td className="px-6 py-3">
                                                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs font-mono">{field.type}</span>
                                                    </td>
                                                    <td className="px-6 py-3">
                                                        {field.required
                                                            ? <span className="text-xs font-medium text-amber-600">required</span>
                                                            : <span className="text-xs text-slate-400">optional</span>
                                                        }
                                                    </td>
                                                    <td className="px-6 py-3 text-slate-600">{field.description}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Output Schema */}
                        {selectedTool.outputSchema && Object.keys(selectedTool.outputSchema).length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-slate-50">
                                    <h3 className="font-semibold text-slate-800">Output Schema</h3>
                                </div>
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-slate-100">
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Field</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Type</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Required</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Description</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {extractSchemaFields(selectedTool.outputSchema).map((field) => (
                                                <tr key={field.name} className="border-b border-slate-50 hover:bg-slate-50">
                                                    <td className="px-6 py-3 font-mono text-sm text-emerald-600">{field.name}</td>
                                                    <td className="px-6 py-3">
                                                        <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs font-mono">{field.type}</span>
                                                    </td>
                                                    <td className="px-6 py-3">
                                                        {field.required
                                                            ? <span className="text-xs font-medium text-amber-600">required</span>
                                                            : <span className="text-xs text-slate-400">optional</span>
                                                        }
                                                    </td>
                                                    <td className="px-6 py-3 text-slate-600">{field.description}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Error Descriptions */}
                        {selectedTool.errorDescriptions && Object.keys(selectedTool.errorDescriptions).length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-red-50">
                                    <h3 className="font-semibold text-slate-800">Error Descriptions</h3>
                                </div>
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-slate-100">
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Error Code</th>
                                                <th className="text-left px-6 py-3 text-xs font-medium text-slate-500 uppercase tracking-wider">Description</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {Object.entries(selectedTool.errorDescriptions).map(([code, desc]) => (
                                                <tr key={code} className="border-b border-slate-50 hover:bg-slate-50">
                                                    <td className="px-6 py-3 font-mono text-sm text-red-600">{code}</td>
                                                    <td className="px-6 py-3 text-slate-600">{desc}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {/* Implementation Class */}
                        {selectedTool.implementationClass && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
                                <h3 className="font-semibold text-slate-800 mb-2">Implementation</h3>
                                <code className="text-sm text-slate-600 bg-slate-50 px-3 py-1.5 rounded font-mono">
                                    {selectedTool.implementationClass}
                                </code>
                            </div>
                        )}
                    </div>
                </main>
            </>
         )}

         {/* Markdown Validation View */}
         {showMarkdownValidator && (
            <>
                <header className="h-16 px-8 flex items-center justify-between bg-white border-b border-slate-200 shrink-0">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 flex items-center">
                            <FileText size={18} className="mr-2 text-indigo-500" />
                            Validate Markdown
                        </h2>
                        <p className="text-xs text-slate-500">Paste or edit skill markdown content, then submit to validate</p>
                    </div>
                    {markdownReport && (
                        <div className="text-xs text-slate-400">
                            Validated in {markdownReport.validationTimeMs}ms
                        </div>
                    )}
                </header>

                <main className="flex-1 overflow-y-auto p-4 md:p-8">
                    <div className="max-w-3xl mx-auto pb-20 space-y-6">

                        {/* Editor */}
                        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                            <div className="px-6 py-4 border-b border-slate-100 bg-slate-50 flex items-center justify-between">
                                <h3 className="font-semibold text-slate-800">Skill Markdown</h3>
                                <span className="text-xs text-slate-400 font-mono">
                                    {markdownInput.split('\n').length} lines
                                </span>
                            </div>
                            <textarea
                                value={markdownInput}
                                onChange={(e) => setMarkdownInput(e.target.value)}
                                placeholder={"---\nid: my_skill\nversion: 1.0.0\n---\n\n## description\nDescribe your skill here...\n\n## input\n```yaml\nquery:\n  type: string\n  required: true\n```\n\n## steps\n\n### step: process\n**type**: prompt\n**varName**: result\n\n```prompt\nProcess the query: {{query}}\n```\n\n## output\n```yaml\nresult:\n  type: string\n```"}
                                className="w-full h-80 px-6 py-4 text-sm font-mono text-slate-800 bg-white border-none resize-y focus:outline-none focus:ring-0 placeholder:text-slate-300"
                                spellCheck={false}
                            />
                            <div className="px-6 py-3 border-t border-slate-100 bg-slate-50 flex items-center justify-between">
                                <div className="text-xs text-slate-400">
                                    {markdownInput.trim() ? `${markdownInput.length} characters` : 'Enter skill markdown content above'}
                                </div>
                                <button
                                    onClick={handleValidateMarkdown}
                                    disabled={!markdownInput.trim() || markdownValidating}
                                    className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                                        !markdownInput.trim() || markdownValidating
                                            ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
                                            : 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm'
                                    }`}
                                >
                                    {markdownValidating ? (
                                        <>
                                            <Loader2 size={14} className="animate-spin" />
                                            Validating...
                                        </>
                                    ) : (
                                        <>
                                            <Send size={14} />
                                            Validate
                                        </>
                                    )}
                                </button>
                            </div>
                        </div>

                        {/* Error */}
                        {markdownError && (
                            <div className="bg-red-50 p-4 rounded-xl border border-red-200 flex items-start space-x-3">
                                <AlertTriangle className="text-red-500 mt-0.5 shrink-0" size={18} />
                                <div>
                                    <h4 className="font-semibold text-sm text-red-800">Validation Request Failed</h4>
                                    <p className="text-red-600 text-sm mt-1">{markdownError}</p>
                                </div>
                            </div>
                        )}

                        {/* Validation Report (reuse existing report rendering) */}
                        {markdownReport && (
                            <>
                                {/* Valid / Invalid Badge */}
                                <div className={`flex items-center gap-2 px-4 py-3 rounded-xl border ${
                                    markdownReport.valid
                                        ? 'bg-emerald-50 border-emerald-200'
                                        : 'bg-red-50 border-red-200'
                                }`}>
                                    {markdownReport.valid
                                        ? <CheckCircle size={18} className="text-emerald-500" />
                                        : <XCircle size={18} className="text-red-500" />
                                    }
                                    <span className={`text-sm font-semibold ${markdownReport.valid ? 'text-emerald-700' : 'text-red-700'}`}>
                                        {markdownReport.valid ? 'Skill is valid' : 'Skill has validation errors'}
                                    </span>
                                    {markdownReport.summary?.skillId && (
                                        <span className="text-xs font-mono px-2 py-0.5 bg-white/60 rounded text-slate-600">
                                            {markdownReport.summary.skillId}
                                            {markdownReport.summary.version && ` v${markdownReport.summary.version}`}
                                        </span>
                                    )}
                                </div>

                                {/* Summary Card */}
                                {markdownReport.summary && (
                                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
                                        <h3 className="font-semibold text-slate-800 mb-4">Skill Summary</h3>
                                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                            <div className="text-center p-3 bg-slate-50 rounded-lg">
                                                <div className="text-2xl font-bold text-slate-800">{markdownReport.summary.stepCount}</div>
                                                <div className="text-xs text-slate-500 mt-1">Steps</div>
                                            </div>
                                            <div className="text-center p-3 bg-slate-50 rounded-lg">
                                                <div className="text-2xl font-bold text-slate-800">{markdownReport.summary.inputFieldCount}</div>
                                                <div className="text-xs text-slate-500 mt-1">Input Fields</div>
                                            </div>
                                            <div className="text-center p-3 bg-slate-50 rounded-lg">
                                                <div className="text-2xl font-bold text-slate-800">{markdownReport.summary.outputFieldCount}</div>
                                                <div className="text-xs text-slate-500 mt-1">Output Fields</div>
                                            </div>
                                            <div className="text-center p-3 bg-slate-50 rounded-lg">
                                                <div className="text-2xl font-bold text-slate-800">{markdownReport.issues.length}</div>
                                                <div className="text-xs text-slate-500 mt-1">Total Issues</div>
                                            </div>
                                        </div>
                                        <div className="flex flex-wrap gap-2 mt-4">
                                            {markdownReport.summary.stepTypes.map((type: string, i: number) => (
                                                <span key={i} className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-indigo-50 text-indigo-600 rounded">
                                                    {type}
                                                </span>
                                            ))}
                                            {markdownReport.summary.hasConditionalSteps && (
                                                <span className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-amber-50 text-amber-600 rounded">
                                                    Conditional
                                                </span>
                                            )}
                                            {markdownReport.summary.hasAwaitSteps && (
                                                <span className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-purple-50 text-purple-600 rounded">
                                                    Await
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                )}

                                {/* Issues Overview */}
                                <div className="flex gap-3">
                                    {markdownReport.errorCount > 0 && (
                                        <div className="flex-1 bg-red-50 border border-red-200 rounded-xl p-4 text-center">
                                            <XCircle size={20} className="mx-auto text-red-500 mb-1" />
                                            <div className="text-xl font-bold text-red-700">{markdownReport.errorCount}</div>
                                            <div className="text-xs text-red-500">Errors</div>
                                        </div>
                                    )}
                                    {markdownReport.warningCount > 0 && (
                                        <div className="flex-1 bg-amber-50 border border-amber-200 rounded-xl p-4 text-center">
                                            <AlertTriangle size={20} className="mx-auto text-amber-500 mb-1" />
                                            <div className="text-xl font-bold text-amber-700">{markdownReport.warningCount}</div>
                                            <div className="text-xs text-amber-500">Warnings</div>
                                        </div>
                                    )}
                                    {markdownReport.suggestionCount > 0 && (
                                        <div className="flex-1 bg-blue-50 border border-blue-200 rounded-xl p-4 text-center">
                                            <Info size={20} className="mx-auto text-blue-500 mb-1" />
                                            <div className="text-xl font-bold text-blue-700">{markdownReport.suggestionCount}</div>
                                            <div className="text-xs text-blue-500">Suggestions</div>
                                        </div>
                                    )}
                                    {markdownReport.errorCount === 0 && markdownReport.warningCount === 0 && markdownReport.suggestionCount === 0 && (
                                        <div className="flex-1 bg-emerald-50 border border-emerald-200 rounded-xl p-6 text-center">
                                            <CheckCircle size={32} className="mx-auto text-emerald-500 mb-2" />
                                            <div className="text-lg font-semibold text-emerald-700">All Checks Passed</div>
                                            <div className="text-sm text-emerald-500">No issues found.</div>
                                        </div>
                                    )}
                                </div>

                                {/* Errors */}
                                {markdownReport.errors.length > 0 && (
                                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                        <div className="px-6 py-4 border-b border-slate-100 bg-red-50">
                                            <h3 className="font-semibold text-red-800 flex items-center gap-2">
                                                <XCircle size={16} />
                                                Errors ({markdownReport.errors.length})
                                            </h3>
                                        </div>
                                        <div className="divide-y divide-slate-100">
                                            {markdownReport.errors.map((issue: ValidationIssue, idx: number) => (
                                                <div key={idx} className="px-6 py-4">
                                                    <div className="flex items-start gap-3">
                                                        <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-red-100 text-red-700 rounded text-[10px] font-mono font-bold">
                                                            {issue.code}
                                                        </span>
                                                        <div className="flex-1">
                                                            <p className="text-sm text-slate-800">{issue.message}</p>
                                                            <div className="flex flex-wrap gap-2 mt-1.5">
                                                                <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                                    {issue.category}
                                                                </span>
                                                                <span className="text-[10px] font-mono text-slate-400">
                                                                    {issue.location}
                                                                </span>
                                                            </div>
                                                            {issue.suggestion && (
                                                                <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                                    Suggestion: {issue.suggestion}
                                                                </p>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Warnings */}
                                {markdownReport.warnings.length > 0 && (
                                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                        <div className="px-6 py-4 border-b border-slate-100 bg-amber-50">
                                            <h3 className="font-semibold text-amber-800 flex items-center gap-2">
                                                <AlertTriangle size={16} />
                                                Warnings ({markdownReport.warnings.length})
                                            </h3>
                                        </div>
                                        <div className="divide-y divide-slate-100">
                                            {markdownReport.warnings.map((issue: ValidationIssue, idx: number) => (
                                                <div key={idx} className="px-6 py-4">
                                                    <div className="flex items-start gap-3">
                                                        <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-amber-100 text-amber-700 rounded text-[10px] font-mono font-bold">
                                                            {issue.code}
                                                        </span>
                                                        <div className="flex-1">
                                                            <p className="text-sm text-slate-800">{issue.message}</p>
                                                            <div className="flex flex-wrap gap-2 mt-1.5">
                                                                <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                                    {issue.category}
                                                                </span>
                                                                <span className="text-[10px] font-mono text-slate-400">
                                                                    {issue.location}
                                                                </span>
                                                            </div>
                                                            {issue.suggestion && (
                                                                <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                                    Suggestion: {issue.suggestion}
                                                                </p>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Suggestions */}
                                {markdownReport.suggestions.length > 0 && (
                                    <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                        <div className="px-6 py-4 border-b border-slate-100 bg-blue-50">
                                            <h3 className="font-semibold text-blue-800 flex items-center gap-2">
                                                <Info size={16} />
                                                Suggestions ({markdownReport.suggestions.length})
                                            </h3>
                                        </div>
                                        <div className="divide-y divide-slate-100">
                                            {markdownReport.suggestions.map((issue: ValidationIssue, idx: number) => (
                                                <div key={idx} className="px-6 py-4">
                                                    <div className="flex items-start gap-3">
                                                        <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-blue-100 text-blue-700 rounded text-[10px] font-mono font-bold">
                                                            {issue.code}
                                                        </span>
                                                        <div className="flex-1">
                                                            <p className="text-sm text-slate-800">{issue.message}</p>
                                                            <div className="flex flex-wrap gap-2 mt-1.5">
                                                                <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                                    {issue.category}
                                                                </span>
                                                                <span className="text-[10px] font-mono text-slate-400">
                                                                    {issue.location}
                                                                </span>
                                                            </div>
                                                            {issue.suggestion && (
                                                                <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                                    Suggestion: {issue.suggestion}
                                                                </p>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </>
                        )}

                    </div>
                </main>
            </>
         )}

         {/* Validation Report Detail View */}
         {selectedReport && (
            <>
                {/* Header */}
                <header className="h-16 px-8 flex items-center justify-between bg-white border-b border-slate-200 shrink-0">
                    <div>
                        <h2 className="text-lg font-bold text-slate-800 flex items-center">
                            <ShieldCheck size={18} className="mr-2 text-indigo-500" />
                            {selectedReport.summary?.skillId || 'Unknown Skill'}
                            {selectedReport.summary?.version && (
                                <span className="ml-2 px-2 py-0.5 rounded text-xs font-mono font-normal bg-indigo-50 text-indigo-600 border border-indigo-100">
                                    v{selectedReport.summary.version}
                                </span>
                            )}
                            {selectedReport.valid
                                ? <span className="ml-2 px-2 py-0.5 rounded text-xs font-normal bg-emerald-50 text-emerald-600 border border-emerald-200">VALID</span>
                                : <span className="ml-2 px-2 py-0.5 rounded text-xs font-normal bg-red-50 text-red-600 border border-red-200">INVALID</span>
                            }
                        </h2>
                        <p className="text-xs text-slate-500">{selectedReport.summary?.description || ''}</p>
                    </div>
                    <div className="text-xs text-slate-400">
                        Validated in {selectedReport.validationTimeMs}ms
                    </div>
                </header>

                {/* Report Content */}
                <main className="flex-1 overflow-y-auto p-4 md:p-8">
                    <div className="max-w-3xl mx-auto pb-20 space-y-6">

                        {/* Summary Card */}
                        {selectedReport.summary && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
                                <h3 className="font-semibold text-slate-800 mb-4">Skill Summary</h3>
                                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                    <div className="text-center p-3 bg-slate-50 rounded-lg">
                                        <div className="text-2xl font-bold text-slate-800">{selectedReport.summary.stepCount}</div>
                                        <div className="text-xs text-slate-500 mt-1">Steps</div>
                                    </div>
                                    <div className="text-center p-3 bg-slate-50 rounded-lg">
                                        <div className="text-2xl font-bold text-slate-800">{selectedReport.summary.inputFieldCount}</div>
                                        <div className="text-xs text-slate-500 mt-1">Input Fields</div>
                                    </div>
                                    <div className="text-center p-3 bg-slate-50 rounded-lg">
                                        <div className="text-2xl font-bold text-slate-800">{selectedReport.summary.outputFieldCount}</div>
                                        <div className="text-xs text-slate-500 mt-1">Output Fields</div>
                                    </div>
                                    <div className="text-center p-3 bg-slate-50 rounded-lg">
                                        <div className="text-2xl font-bold text-slate-800">{selectedReport.issues.length}</div>
                                        <div className="text-xs text-slate-500 mt-1">Total Issues</div>
                                    </div>
                                </div>
                                <div className="flex flex-wrap gap-2 mt-4">
                                    {selectedReport.summary.stepTypes.map((type, i) => (
                                        <span key={i} className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-indigo-50 text-indigo-600 rounded">
                                            {type}
                                        </span>
                                    ))}
                                    {selectedReport.summary.hasConditionalSteps && (
                                        <span className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-amber-50 text-amber-600 rounded">
                                            Conditional
                                        </span>
                                    )}
                                    {selectedReport.summary.hasAwaitSteps && (
                                        <span className="text-[10px] uppercase font-medium px-1.5 py-0.5 bg-purple-50 text-purple-600 rounded">
                                            Await
                                        </span>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Issues Overview */}
                        <div className="flex gap-3">
                            {selectedReport.errorCount > 0 && (
                                <div className="flex-1 bg-red-50 border border-red-200 rounded-xl p-4 text-center">
                                    <XCircle size={20} className="mx-auto text-red-500 mb-1" />
                                    <div className="text-xl font-bold text-red-700">{selectedReport.errorCount}</div>
                                    <div className="text-xs text-red-500">Errors</div>
                                </div>
                            )}
                            {selectedReport.warningCount > 0 && (
                                <div className="flex-1 bg-amber-50 border border-amber-200 rounded-xl p-4 text-center">
                                    <AlertTriangle size={20} className="mx-auto text-amber-500 mb-1" />
                                    <div className="text-xl font-bold text-amber-700">{selectedReport.warningCount}</div>
                                    <div className="text-xs text-amber-500">Warnings</div>
                                </div>
                            )}
                            {selectedReport.suggestionCount > 0 && (
                                <div className="flex-1 bg-blue-50 border border-blue-200 rounded-xl p-4 text-center">
                                    <Info size={20} className="mx-auto text-blue-500 mb-1" />
                                    <div className="text-xl font-bold text-blue-700">{selectedReport.suggestionCount}</div>
                                    <div className="text-xs text-blue-500">Suggestions</div>
                                </div>
                            )}
                            {selectedReport.errorCount === 0 && selectedReport.warningCount === 0 && selectedReport.suggestionCount === 0 && (
                                <div className="flex-1 bg-emerald-50 border border-emerald-200 rounded-xl p-6 text-center">
                                    <CheckCircle size={32} className="mx-auto text-emerald-500 mb-2" />
                                    <div className="text-lg font-semibold text-emerald-700">All Checks Passed</div>
                                    <div className="text-sm text-emerald-500">No issues found in this skill.</div>
                                </div>
                            )}
                        </div>

                        {/* Errors */}
                        {selectedReport.errors.length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-red-50">
                                    <h3 className="font-semibold text-red-800 flex items-center gap-2">
                                        <XCircle size={16} />
                                        Errors ({selectedReport.errors.length})
                                    </h3>
                                </div>
                                <div className="divide-y divide-slate-100">
                                    {selectedReport.errors.map((issue: ValidationIssue, idx: number) => (
                                        <div key={idx} className="px-6 py-4">
                                            <div className="flex items-start gap-3">
                                                <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-red-100 text-red-700 rounded text-[10px] font-mono font-bold">
                                                    {issue.code}
                                                </span>
                                                <div className="flex-1">
                                                    <p className="text-sm text-slate-800">{issue.message}</p>
                                                    <div className="flex flex-wrap gap-2 mt-1.5">
                                                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                            {issue.category}
                                                        </span>
                                                        <span className="text-[10px] font-mono text-slate-400">
                                                            {issue.location}
                                                        </span>
                                                    </div>
                                                    {issue.suggestion && (
                                                        <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                            Suggestion: {issue.suggestion}
                                                        </p>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Warnings */}
                        {selectedReport.warnings.length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-amber-50">
                                    <h3 className="font-semibold text-amber-800 flex items-center gap-2">
                                        <AlertTriangle size={16} />
                                        Warnings ({selectedReport.warnings.length})
                                    </h3>
                                </div>
                                <div className="divide-y divide-slate-100">
                                    {selectedReport.warnings.map((issue: ValidationIssue, idx: number) => (
                                        <div key={idx} className="px-6 py-4">
                                            <div className="flex items-start gap-3">
                                                <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-amber-100 text-amber-700 rounded text-[10px] font-mono font-bold">
                                                    {issue.code}
                                                </span>
                                                <div className="flex-1">
                                                    <p className="text-sm text-slate-800">{issue.message}</p>
                                                    <div className="flex flex-wrap gap-2 mt-1.5">
                                                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                            {issue.category}
                                                        </span>
                                                        <span className="text-[10px] font-mono text-slate-400">
                                                            {issue.location}
                                                        </span>
                                                    </div>
                                                    {issue.suggestion && (
                                                        <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                            Suggestion: {issue.suggestion}
                                                        </p>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Suggestions */}
                        {selectedReport.suggestions.length > 0 && (
                            <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
                                <div className="px-6 py-4 border-b border-slate-100 bg-blue-50">
                                    <h3 className="font-semibold text-blue-800 flex items-center gap-2">
                                        <Info size={16} />
                                        Suggestions ({selectedReport.suggestions.length})
                                    </h3>
                                </div>
                                <div className="divide-y divide-slate-100">
                                    {selectedReport.suggestions.map((issue: ValidationIssue, idx: number) => (
                                        <div key={idx} className="px-6 py-4">
                                            <div className="flex items-start gap-3">
                                                <span className="shrink-0 mt-0.5 px-1.5 py-0.5 bg-blue-100 text-blue-700 rounded text-[10px] font-mono font-bold">
                                                    {issue.code}
                                                </span>
                                                <div className="flex-1">
                                                    <p className="text-sm text-slate-800">{issue.message}</p>
                                                    <div className="flex flex-wrap gap-2 mt-1.5">
                                                        <span className="text-[10px] font-medium px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded">
                                                            {issue.category}
                                                        </span>
                                                        <span className="text-[10px] font-mono text-slate-400">
                                                            {issue.location}
                                                        </span>
                                                    </div>
                                                    {issue.suggestion && (
                                                        <p className="text-xs text-emerald-600 mt-2 bg-emerald-50 px-3 py-1.5 rounded">
                                                            Suggestion: {issue.suggestion}
                                                        </p>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                    </div>
                </main>
            </>
         )}

      </div>
    </div>
  );
};

export default App;
