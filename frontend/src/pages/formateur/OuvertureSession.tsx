import { useState } from "react";
import { Alert, Button, Card, Form, Input, InputNumber, Spin, Typography } from "antd";
import { api, SessionOuverte } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

const { Title, Text } = Typography;

interface FormValues {
  titre: string;
  promotionId: number;
}

/**
 * EF1 : le formateur ouvre une session et obtient un code de présence.
 */
export function OuvertureSession() {
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);
  const [session, setSession] = useState<SessionOuverte | null>(null);

  async function ouvrirSession(valeurs: FormValues) {
    setChargement(true);
    setErreur(null);
    try {
      const reponse = await api.ouvrirSession({
        titre: valeurs.titre,
        promotionId: valeurs.promotionId,
      });
      setSession(reponse);
    } catch (cause) {
      const message = cause instanceof ErreurApi ? cause.message : "Une erreur inattendue est survenue.";
      setErreur(message);
    } finally {
      setChargement(false);
    }
  }

  return (
    <Card style={{ maxWidth: 480, margin: "2rem auto" }}>
      <Title level={3}>Ouvrir une session</Title>

      <Form<FormValues> layout="vertical" onFinish={ouvrirSession} disabled={chargement}>
        <Form.Item name="titre" label="Titre de la session" rules={[{ required: true, message: "Le titre est obligatoire." }]}>
          <Input placeholder="Ex. Cours Java — chapitre 3" />
        </Form.Item>
        <Form.Item name="promotionId" label="Promotion" rules={[{ required: true, message: "La promotion est obligatoire." }]}>
          <InputNumber style={{ width: "100%" }} min={1} placeholder="Identifiant de la promotion" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" loading={chargement} block>
            Ouvrir la session
          </Button>
        </Form.Item>
      </Form>

      {chargement && <Spin />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginTop: 16 }} />}

      {session && (
        <Card type="inner" style={{ marginTop: 24, textAlign: "center" }}>
          <Text type="secondary">Code de présence</Text>
          <div style={{ fontSize: 48, fontWeight: 700, letterSpacing: 8 }}>{session.code}</div>
          <Text type="secondary">
            Expire à {new Date(session.expirationAt).toLocaleTimeString("fr-FR")}
          </Text>
        </Card>
      )}
    </Card>
  );
}
