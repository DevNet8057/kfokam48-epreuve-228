import { useState } from "react";
import { Alert, Button, Card, Form, Input, Result, Spin } from "antd";
import { api } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";
import { useIdentite } from "../../identite/IdentiteContext";

interface Props {
  sessionId: number;
}

interface FormValues {
  lien: string;
}

/**
 * EF6 : dépôt du lien de l'exercice pour la session (RG10 : possible même absent).
 */
export function DeposerExercice({ sessionId }: Props) {
  const { identite } = useIdentite();
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [statut, setStatut] = useState<string | null>(null);

  async function deposer(valeurs: FormValues) {
    if (!identite?.etudiantId) return;
    setChargement(true);
    setErreur(null);
    try {
      const exercice = await api.deposerExercice({ sessionId, etudiantId: identite.etudiantId, lien: valeurs.lien });
      setStatut(exercice.statut);
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Une erreur inattendue est survenue.");
    } finally {
      setChargement(false);
    }
  }

  if (statut) {
    return (
      <Card style={{ maxWidth: 480, margin: "1rem auto" }}>
        <Result
          status="success"
          title="Exercice déposé"
          subTitle={statut === "SANS_RELECTEUR" ? "Aucun relecteur disponible pour l'instant." : "Un relecteur vous a été attribué."}
        />
      </Card>
    );
  }

  return (
    <Card style={{ maxWidth: 480, margin: "1rem auto" }} title="Déposer mon exercice">
      <Form<FormValues> layout="vertical" onFinish={deposer} disabled={chargement}>
        <Form.Item name="lien" label="Lien de l'exercice" rules={[{ required: true, message: "Le lien est obligatoire." }]}>
          <Input placeholder="https://…" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={chargement} block>
            Déposer
          </Button>
        </Form.Item>
      </Form>
      {chargement && <Spin />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginTop: 16 }} />}
    </Card>
  );
}
