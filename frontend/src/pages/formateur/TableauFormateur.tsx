import { useEffect, useState } from "react";
import { Alert, Card, Select } from "antd";
import { api, Promotion } from "../../api/client";
import { TableauDonnees } from "./TableauDonnees";

/**
 * EF10 : tableau de présence et de notes du formateur. La moyenne vient uniquement de l'API (F3, RG18).
 * Le rafraîchissement des données est délégué à TableauDonnees (K48-26).
 */
export function TableauFormateur() {
  const [promotions, setPromotions] = useState<Promotion[]>([]);
  const [promotionId, setPromotionId] = useState<number | null>(null);
  const [erreur, setErreur] = useState<string | null>(null);

  useEffect(() => {
    api.listerPromotions().then(setPromotions).catch(() => setErreur("Impossible de charger les promotions."));
  }, []);

  return (
    <Card style={{ maxWidth: 900, margin: "1rem auto" }} title="Tableau de la promotion">
      <Select
        style={{ width: "100%", marginBottom: 16 }}
        placeholder="Choisir une promotion"
        options={promotions.map((p) => ({ value: p.id, label: p.nom }))}
        onChange={(valeur) => setPromotionId(valeur)}
      />
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
      {promotionId !== null && <TableauDonnees promotionId={promotionId} />}
    </Card>
  );
}
