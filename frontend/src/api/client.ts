import { ErreurApi } from "./erreurApi";

// Couche API unique (F3) : seul module du frontend à faire des appels réseau.
// Base URL configurable par variable d'environnement (VITE_API_URL), en-tête X-Etudiant-Id ajouté
// automatiquement à partir de l'identité courante (K48-25), sauf si l'appelant le précise explicitement.
const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

let etudiantIdCourant: number | undefined;

/** Appelé par IdentiteContext lors du choix ou du changement d'identité. */
export function definirEtudiantIdCourant(id: number | undefined) {
  etudiantIdCourant = id;
}

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
  const etudiantId = options?.etudiantId ?? etudiantIdCourant;
  if (etudiantId !== undefined) {
    entetes["X-Etudiant-Id"] = String(etudiantId);
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

export interface Promotion {
  id: number;
  nom: string;
}

export interface Etudiant {
  id: number;
  nom: string;
  promotionId: number;
}

export interface MarquerPresenceRequete {
  code: string;
  etudiantId: number;
}

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: "ETUDIANT" | "FORMATEUR";
}

export const api = {
  ouvrirSession: (donnees: OuvrirSessionRequete) =>
    requete<SessionOuverte>("POST", "/api/sessions", donnees),
  listerPromotions: () => requete<Promotion[]>("GET", "/api/promotions"),
  listerEtudiantsDeLaPromotion: (promotionId: number) =>
    requete<Etudiant[]>("GET", `/api/promotions/${promotionId}/etudiants`),
  marquerPresence: (donnees: MarquerPresenceRequete) =>
    requete<Presence>("POST", "/api/presences", donnees),
};
