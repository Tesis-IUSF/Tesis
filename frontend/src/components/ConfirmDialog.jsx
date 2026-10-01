import "./ConfirmDialog.css";
//(reemplazo del window.confirm nativo)
function ConfirmDialog({
  open,
  title,
  message,
  onConfirm,
  onCancel,
  cargando,
}) {
  if (!open) return null;

  return (
    <div className="modal-overlay active" onClick={onCancel}>
      <div className="modal active" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button
            className="modal-close"
            onClick={onCancel}
            aria-label="Cerrar"
          >
            ×
          </button>
        </div>
        <div className="modal-body">
          <p>{message}</p>
        </div>
        <div className="modal-footer">
          <button
            className="btn-secondary"
            onClick={onCancel}
            disabled={cargando}
          >
            Cancelar
          </button>
          <button
            className="btn-delete-confirm"
            onClick={onConfirm}
            disabled={cargando}
          >
            {cargando ? "Eliminando..." : "Sí, eliminar"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmDialog;
