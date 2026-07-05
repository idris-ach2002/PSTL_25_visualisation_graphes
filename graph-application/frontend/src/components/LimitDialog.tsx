type Props = {
  title: string;
  message: string;
  advice: string;
  details?: string;
  onClose: () => void;
  onImportAnother: () => void;
};

export function LimitDialog({ title, message, advice, details, onClose, onImportAnother }: Props) {
  return (
    <div className="modalBackdrop" role="dialog" aria-modal="true" aria-labelledby="limit-title">
      <div className="limitModal">
        <div className="modalIcon">!</div>
        <div className="modalContent">
          <h2 id="limit-title">{title}</h2>
          <p>{message}</p>
          <p className="modalAdvice">{advice}</p>
          {details && <p className="modalDetails">{details}</p>}
          <div className="modalActions">
            <button type="button" className="primaryButton" onClick={onImportAnother}>Importer un autre fichier</button>
            <button type="button" onClick={onClose}>Fermer</button>
          </div>
        </div>
      </div>
    </div>
  );
}
