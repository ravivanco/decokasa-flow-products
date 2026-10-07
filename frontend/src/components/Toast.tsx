import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info';
  text: string;
}

interface ToastProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}

export const ToastContainer: React.FC<ToastProps> = ({ toasts, onDismiss }) => {
  if (toasts.length === 0) return null;

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-md w-full px-4 pointer-events-none">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl shadow-xl border text-sm transition-all duration-300 animate-in slide-in-from-bottom-5 ${
            toast.type === 'success'
              ? 'bg-[#515151] text-white border-[#16A34A]'
              : toast.type === 'error'
              ? 'bg-[#515151] text-white border-[#DC2626]'
              : 'bg-[#515151] text-white border-[#6C6B6D]'
          }`}
        >
          {toast.type === 'success' && <CheckCircle2 className="w-5 h-5 text-[#16A34A] shrink-0 mt-0.5" />}
          {toast.type === 'error' && <AlertCircle className="w-5 h-5 text-[#DC2626] shrink-0 mt-0.5" />}
          {toast.type === 'info' && <Info className="w-5 h-5 text-[#FFD100] shrink-0 mt-0.5" />}
          
          <div className="flex-1 text-white font-medium leading-snug">
            {toast.text}
          </div>

          <button
            onClick={() => onDismiss(toast.id)}
            className="text-slate-300 hover:text-white transition-colors p-0.5 rounded"
            aria-label="Cerrar notificación"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      ))}
    </div>
  );
};
