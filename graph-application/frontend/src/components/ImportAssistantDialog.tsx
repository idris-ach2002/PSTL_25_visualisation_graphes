import type { ImportAnalysis, ImportSampleMode } from '../types/graph';

type Props = {
  analysis: ImportAnalysis;
  limit: number;
  maxLimit: number;
  onChangeLimit: (limit: number) => void;
  onImport: () => void;
  onSample: (mode: ImportSampleMode) => void;
  onCancel: () => void;
  onImportAnother: () => void;
};

function format(value: number): string {
  return value.toLocaleString('fr-FR');
}

export function ImportAssistantDialog({ analysis, limit, maxLimit, onChangeLimit, onImport, onSample, onCancel, onImportAnother }: Props) {
  return (
    <div className="modalBackdrop" role="dialog" aria-modal="true" aria-labelledby="importTitle">
      <section className="importAssistant modalCard largeModal">
        <header className="modalHeader">
          <div>
            <h2 id="importTitle">Assistant d’import</h2>
            <p>{analysis.fileName}</p>
          </div>
          <span className={`statusPill ${analysis.severity}`}>{analysis.severity === 'blocked' ? 'Bloqué' : analysis.severity === 'warning' ? 'Attention' : 'Compatible'}</span>
        </header>

        <div className="importGrid">
          <div className="metricBox"><span>Type détecté</span><strong>{analysis.sourceKind}</strong></div>
          <div className="metricBox"><span>Nœuds estimés</span><strong>{format(analysis.nodeCount)}</strong></div>
          <div className="metricBox"><span>Arêtes estimées</span><strong>{format(analysis.edgeCount)}</strong></div>
          <div className="metricBox"><span>Limite active</span><strong>{format(limit)}</strong></div>
        </div>

        <p className={analysis.accepted ? 'successText' : 'errorText'}>{analysis.message}</p>
        <p className="mutedText">{analysis.advice}</p>
        {analysis.estimatedMemory && <p className="mutedText">Mémoire estimée : {analysis.estimatedMemory}</p>}

        <label className="fieldControl limitField">
          <span>Limite web paramétrable</span>
          <input
            type="number"
            min={100}
            max={maxLimit}
            step={50}
            value={limit}
            onChange={(event) => onChangeLimit(Math.max(100, Math.min(maxLimit, Number(event.target.value) || 100)))}
          />
          <small>Maximum conseillé : {format(maxLimit)} nœuds pour garder une app web fluide.</small>
        </label>

        {analysis.warnings.length > 0 && (
          <div className="warningList">
            <strong>Points détectés</strong>
            {analysis.warnings.map((warning) => <span key={warning}>{warning}</span>)}
          </div>
        )}

        <div className="samplingBox">
          <strong>Si le fichier dépasse la limite</strong>
          <p>Tu peux générer un échantillon compatible sans modifier le fichier original.</p>
          <div className="buttonRow three">
            <button type="button" onClick={() => onSample('first')}>Garder les premiers</button>
            <button type="button" onClick={() => onSample('random')}>Échantillon aléatoire</button>
            <button type="button" onClick={() => onSample('top-degree')}>Nœuds les plus connectés</button>
          </div>
        </div>

        <footer className="modalActions">
          <button type="button" onClick={onCancel}>Annuler</button>
          <button type="button" onClick={onImportAnother}>Autre fichier</button>
          <button type="button" className="primary" disabled={!analysis.accepted} onClick={onImport}>Importer ce graphe</button>
        </footer>
      </section>
    </div>
  );
}
