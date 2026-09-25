import { useEffect, useState } from "react";
import { Alert, Card, Empty, List, Result, Spin } from "antd";
import { api, RelectureResume } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";
import { useIdentite } from "../../identite/IdentiteContext";
import { RendreRelecture } from "./RendreRelecture";

/**
 * EF8/EF9 : liste des exercices assignés au relecteur, non encore rendus, sans le nom de l'auteur.
 * L'ouverture (H9) enregistre ouverte_at et passe l'exercice EN_COURS_RELECTURE avant de suivre le lien.
 */
export function ListeRelectures() {
  const { identite } = useIdentite();
  const [relectures, setRelectures] = useState<RelectureResume[] | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);
  const [ouvertureId, setOuvertureId] = useState<number | null>(null);
  const [rendueId, setRendueId] = useState<number | null>(null);

  useEffect(() => {
    if (!identite?.etudiantId) return;
    let annule = false;
    api
      .listerRelecturesAFaire(identite.etudiantId)
      .then((reponse) => {
        if (!annule) setRelectures(reponse);
      })
      .catch((cause) => {
        if (!annule) setErreur(cause instanceof ErreurApi ? cause.message : "Impossible de charger les relectures.");
      });
    return () => {
      annule = true;
    };
  }, [identite?.etudiantId]);

  async function ouvrirEtSuivre(relecture: RelectureResume) {
    setOuvertureId(relecture.id);
    try {
      await api.ouvrirRelecture(relecture.id);
    } catch {
      // L'ouverture échoue silencieusement côté enregistrement : le lien reste consultable.
    }
    window.open(relecture.lien, "_blank", "noreferrer");
  }

  function retirerDeLaListe(relectureId: number) {
    setRelectures((actuel) => actuel?.filter((relecture) => relecture.id !== relectureId) ?? null);
    setRendueId(relectureId);
  }

  if (rendueId !== null) {
    return (
      <Card style={{ maxWidth: 480, margin: "2rem auto" }}>
        <Result status="success" title="Relecture rendue" />
      </Card>
    );
  }

  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }} title="Exercices à relire">
      {erreur && <Alert type="error" message={erreur} showIcon />}
      {!erreur && relectures === null && <Spin />}
      {relectures !== null && relectures.length === 0 && <Empty description="Rien à relire pour l'instant." />}
      {relectures !== null && relectures.length > 0 && (
        <List
          dataSource={relectures}
          renderItem={(relecture) => (
            <List.Item style={{ flexDirection: "column", alignItems: "stretch", gap: 8 }}>
              <a href={relecture.lien} onClick={(evenement) => { evenement.preventDefault(); ouvrirEtSuivre(relecture); }}>
                {relecture.lien}
              </a>
              {ouvertureId === relecture.id && (
                <RendreRelecture relectureId={relecture.id} onRendue={() => retirerDeLaListe(relecture.id)} />
              )}
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}
