import { useState } from "react";
import { Alert, Button, Form, Input, InputNumber, Spin } from "antd";
import { api } from "../../api/client";
import { ErreurApi } from "../../api/erreurApi";

interface Props {
  relectureId: number;
  onRendue: () => void;
}

interface FormValues {
  note: number;
  commentaire: string;
}

/**
 * EF9 : rendre une note (entière, 0-20) et un commentaire, définitifs (RG3, RG14).
 */
export function RendreRelecture({ relectureId, onRendue }: Props) {
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState<string | null>(null);

  async function rendre(valeurs: FormValues) {
    setChargement(true);
    setErreur(null);
    try {
      await api.rendreRelecture(relectureId, { note: valeurs.note, commentaire: valeurs.commentaire });
      onRendue();
    } catch (cause) {
      setErreur(cause instanceof ErreurApi ? cause.message : "Une erreur inattendue est survenue.");
    } finally {
      setChargement(false);
    }
  }

  return (
    <Form<FormValues> layout="vertical" onFinish={rendre} disabled={chargement} style={{ width: "100%" }}>
      <Form.Item name="note" label="Note (0 à 20)" rules={[{ required: true, message: "La note est obligatoire." }]}>
        <InputNumber min={0} max={20} precision={0} style={{ width: "100%" }} />
      </Form.Item>
      <Form.Item name="commentaire" label="Commentaire" rules={[{ required: true, message: "Le commentaire est obligatoire." }]}>
        <Input.TextArea rows={3} />
      </Form.Item>
      <Form.Item>
        <Button type="primary" htmlType="submit" loading={chargement} block>
          Rendre définitivement
        </Button>
      </Form.Item>
      {chargement && <Spin />}
      {erreur && <Alert type="error" message={erreur} showIcon style={{ marginBottom: 16 }} />}
    </Form>
  );
}
