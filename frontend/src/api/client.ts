import { ErreurApi } from "./erreurApi";

// Couche API unique (F3) : seul module du frontend à faire des appels réseau.
// Base URL configurable par variable d'environnement (VITE_API_URL), en-tête X-Etudiant-Id ajouté quand fourni.
const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

type Options = {
  etudiantId?: number;
};

async function requete<T>(
  method: "GET" | "POST" | "PUT",
  chemin: string,
  corps?: unknown,
  options?: Options
): Promise<T> {
  const entetes: Record<string, string> = { "Content-Type": "application/json" };
  if (options?.etudiantId !== undefined) {
    entetes["X-Etudiant-Id"] = String(options.etudiantId);
  }

  const reponse = await fetch(`${BASE_URL}${chemin}`, {
    method,
    headers: entetes,
    body: corps === undefined ? undefined : JSON.stringify(corps),
  });

  if (reponse.status === 204) {
    return undefined as T;
  }

  const donnees = await reponse.json().catch(() => undefined);

  if (!reponse.ok) {
    const code = donnees?.code ?? "ERREUR_INCONNUE";
    const message = donnees?.message ?? "Une erreur est survenue.";
    throw new ErreurApi(code, message);
  }

  return donnees as T;
}

export interface OuvrirSessionRequete {
  titre: string;
  promotionId: number;
}

export interface SessionOuverte {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

export const api = {
  ouvrirSession: (donnees: OuvrirSessionRequete) =>
    requete<SessionOuverte>("POST", "/api/sessions", donnees),
};
