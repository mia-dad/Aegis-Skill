// ============================================
// 动态表单组件
// ============================================
// 本组件根据后端返回的 InputSchema 动态渲染表单
// 支持多种字段类型和验证规则
// ============================================

import React, { useState, useEffect } from 'react';
import { FieldSpec, InputSchema } from '../types';
import { Info, AlertCircle } from 'lucide-react';

/**
 * DynamicForm 组件属性接口
 *
 * @property schema - 输入参数结构定义，可以是完整的 InputSchema 或简化的字段映射
 * @property onSubmit - 表单提交回调函数，接收用户输入的数据
 * @property isLoading - 是否正在加载，用于禁用提交按钮和显示加载状态
 * @property submitLabel - 提交按钮的文本，默认为 'Execute Skill'
 * @property initialValues - 表单初始值，用于编辑场景或多轮交互
 */
interface DynamicFormProps {
  schema: InputSchema | Record<string, FieldSpec>;
  onSubmit: (data: any) => void;
  isLoading: boolean;
  submitLabel?: string;
  initialValues?: any;
}

/**
 * DynamicForm 组件
 *
 * 核心功能：
 * 1. 根据 schema 动态生成表单字段
 * 2. 支持多种字段类型（text、textarea、select、number、switch 等）
 * 3. 实时验证用户输入
 * 4. 处理字段间的依赖关系
 * 5. 支持必填项和自定义验证规则
 *
 * 支持的字段类型（通过 uiHint）：
 * - text: 单行文本输入
 * - textarea: 多行文本输入
 * - select: 下拉单选
 * - multiselect: 下拉多选
 * - checkbox: 复选框
 * - radio: 单选框
 * - date: 日期选择
 * - datetime: 日期时间选择
 * - number: 数字输入
 * - switch: 开关切换
 */
export const DynamicForm: React.FC<DynamicFormProps> = ({
  schema,
  onSubmit,
  isLoading,
  submitLabel = 'Execute Skill',
  initialValues = {}
}) => {
  const [formData, setFormData] = useState<Record<string, any>>(initialValues);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Reset form when schema changes significantly
  useEffect(() => {
    setFormData(initialValues);
    setErrors({});
  }, [schema]);

  const fields = ('fields' in schema ? schema.fields : schema) as Record<string, FieldSpec>;

  const handleChange = (key: string, value: any) => {
    setFormData(prev => ({ ...prev, [key]: value }));
    // Clear error on change
    if (errors[key]) {
      setErrors(prev => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
    }
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    Object.entries(fields).forEach(([key, spec]) => {
      const value = formData[key];
      
      // Required Check
      if (spec.required) {
        const isEmpty = spec.type === 'boolean'
          ? (value === undefined || value === null)
          : (value === undefined || value === null || value === '');
        if (isEmpty) {
          newErrors[key] = 'This field is required';
          isValid = false;
        }
        if (Array.isArray(value) && value.length === 0) {
            newErrors[key] = 'Please select at least one option';
            isValid = false;
        }
      }

      // Simple Numeric Check
      if (spec.type === 'number' && value !== undefined && value !== '') {
         if (isNaN(Number(value))) {
             newErrors[key] = 'Must be a valid number';
             isValid = false;
         }
      }
    });

    setErrors(newErrors);
    return isValid;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      // Convert number types
      const processedData = { ...formData };
      Object.entries(fields).forEach(([key, spec]) => {
          if (spec.type === 'number' && processedData[key]) {
              processedData[key] = Number(processedData[key]);
          }
      });
      onSubmit(processedData);
    }
  };

  const renderField = (key: string, spec: FieldSpec) => {
    const isError = !!errors[key];
    const baseInputClass = `w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors ${
        isError ? 'border-red-500 focus:ring-red-500' : 'border-slate-300'
    }`;

    let inputElement;

    switch (spec.uiHint) {
      case 'textarea':
        inputElement = (
          <textarea
            className={`${baseInputClass} min-h-[100px]`}
            placeholder={spec.placeholder}
            value={formData[key] || ''}
            onChange={(e) => handleChange(key, e.target.value)}
            disabled={isLoading}
          />
        );
        break;

      case 'select':
        inputElement = (
          <select
            className={`${baseInputClass} bg-white`}
            value={formData[key] || spec.defaultValue || ''}
            onChange={(e) => handleChange(key, e.target.value)}
            disabled={isLoading}
          >
            <option value="" disabled>Select an option</option>
            {spec.options?.map(opt => (
              <option key={opt} value={opt}>{opt}</option>
            ))}
          </select>
        );
        break;

      case 'checkbox':
        inputElement = (
          <div className="flex items-center space-x-2 mt-2">
            <input
              type="checkbox"
              id={key}
              className="w-4 h-4 text-indigo-600 border-slate-300 rounded focus:ring-indigo-500"
              checked={!!formData[key]}
              onChange={(e) => handleChange(key, e.target.checked)}
              disabled={isLoading}
            />
            <label htmlFor={key} className="text-sm text-slate-700 font-medium select-none cursor-pointer">
              {spec.description || key}
            </label>
          </div>
        );
        break;
      
      case 'multiselect':
        const currentSelection = (formData[key] as string[]) || [];
        inputElement = (
           <div className="flex flex-wrap gap-2 mt-2">
               {spec.options?.map(opt => {
                   const isSelected = currentSelection.includes(opt);
                   return (
                       <button
                        key={opt}
                        type="button"
                        disabled={isLoading}
                        onClick={() => {
                            const newSelection = isSelected 
                                ? currentSelection.filter(s => s !== opt)
                                : [...currentSelection, opt];
                            handleChange(key, newSelection);
                        }}
                        className={`px-3 py-1 rounded-full text-sm border transition-colors ${
                            isSelected 
                            ? 'bg-indigo-100 border-indigo-500 text-indigo-700' 
                            : 'bg-white border-slate-300 text-slate-600 hover:bg-slate-50'
                        }`}
                       >
                           {opt} {isSelected && '✓'}
                       </button>
                   )
               })}
           </div>
        );
        break;

      case 'text':
      case 'number':
      default:
        // boolean 类型无 uiHint 时，渲染为是/否单选
        if (spec.type === 'boolean' && !spec.uiHint) {
          const currentVal = formData[key];
          inputElement = (
            <div className="flex items-center space-x-4 mt-2">
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name={key}
                  className="w-4 h-4 text-indigo-600 border-slate-300 focus:ring-indigo-500"
                  checked={currentVal === true}
                  onChange={() => handleChange(key, true)}
                  disabled={isLoading}
                />
                <span className="text-sm text-slate-700 font-medium">是</span>
              </label>
              <label className="flex items-center space-x-2 cursor-pointer">
                <input
                  type="radio"
                  name={key}
                  className="w-4 h-4 text-indigo-600 border-slate-300 focus:ring-indigo-500"
                  checked={currentVal === false}
                  onChange={() => handleChange(key, false)}
                  disabled={isLoading}
                />
                <span className="text-sm text-slate-700 font-medium">否</span>
              </label>
            </div>
          );
        } else {
          inputElement = (
            <input
              type={spec.type === 'number' ? 'number' : 'text'}
              className={baseInputClass}
              placeholder={spec.placeholder}
              value={formData[key] || ''}
              onChange={(e) => handleChange(key, e.target.value)}
              disabled={isLoading}
            />
          );
        }
        break;
    }

    // Special wrapper for checkbox to handle description differently
    if (spec.uiHint === 'checkbox') {
        return (
            <div key={key} className="mb-6">
                 {inputElement}
                 {isError && (
                    <p className="mt-1 text-xs text-red-500 flex items-center">
                        <AlertCircle size={12} className="mr-1" />
                        {errors[key]}
                    </p>
                )}
            </div>
        )
    }

    return (
      <div key={key} className="mb-6">
        <label htmlFor={key} className="block text-sm font-semibold text-slate-700 mb-1">
          {key.charAt(0).toUpperCase() + key.slice(1)}
          {spec.required && <span className="text-red-500 ml-1">*</span>}
        </label>
        
        {spec.description && (
          <p className="text-xs text-slate-500 mb-2 flex items-center">
            <Info size={12} className="mr-1 inline" />
            {spec.description}
          </p>
        )}

        {inputElement}

        {isError && (
          <p className="mt-1 text-xs text-red-500 flex items-center">
            <AlertCircle size={12} className="mr-1" />
            {errors[key]}
          </p>
        )}
      </div>
    );
  };

  return (
    <form onSubmit={handleSubmit} className="w-full">
      {Object.entries(fields).map(([key, spec]) => renderField(key, spec))}
      
      <div className="mt-8 pt-4 border-t border-slate-100 sticky bottom-0 bg-white pb-4">
        <button
          type="submit"
          disabled={isLoading}
          className={`w-full py-3 px-4 rounded-lg font-medium text-white shadow-md transition-all ${
            isLoading 
            ? 'bg-indigo-300 cursor-not-allowed' 
            : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-lg active:scale-[0.99]'
          }`}
        >
          {isLoading ? (
            <span className="flex items-center justify-center">
              <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              Processing...
            </span>
          ) : (
            submitLabel
          )}
        </button>
      </div>
    </form>
  );
};