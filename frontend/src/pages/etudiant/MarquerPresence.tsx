import { useState } from "react";
import { Alert, Button, Card, Form, Input, Result, Spin } from "antd";
import { api } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";
import { useIdentite } from "../../identite/IdentiteContext";
import { DeposerExercice } from "./DeposerExercice";

interface FormValues {
  code: string;
}

/**
 * EF3 : l'étudiant marque sa présence avec le code donné par le formateur (RG1, RG5, RG6).
 */
export function MarquerPresence() {
  const { identite } = useIdentite();
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [sessionId, setSessionId] = useState<number | null>(null);

  async function marquerPresence(valeurs: FormValues) {
    if (!identite?.etudiantId) return;
    setChargement(true);
    setErreur(null);
    try {
      const presence = await api.marquerPresence({ code: valeurs.code, etudiantId: identite.etudiantId });
      setSessionId(presence.sessionId);
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Une erreur inattendue est survenue.");
    } finally {
      setChargement(false);
    }
  }

  if (sessionId !== null) {
    return (
      <>
        <Card style={{ maxWidth: 480, margin: "2rem auto 1rem" }}>
          <Result status="success" title="Présence enregistrée" />
          <Button block onClick={() => setSessionId(null)}>
            Marquer une autre présence
          </Button>
        </Card>
        <DeposerExercice sessionId={sessionId} />
      </>
    );
  }

  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }} title="Marquer ma présence">
      <Form<FormValues> layout="vertical" onFinish={marquerPresence} disabled={chargement}>
        <Form.Item name="code" label="Code de la session" rules={[{ required: true, message: "Le code est obligatoire." }]}>
          <Input placeholder="Ex. C9P6VY" maxLength={6} style={{ textTransform: "uppercase" }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={chargement} block>
            Valider
          </Button>
        </Form.Item>
      </Form>
      {chargement && <Spin />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginTop: 16 }} />}
    </Card>
  );
}
