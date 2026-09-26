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

export interface DeposerExerciceRequete {
  sessionId: number;
  etudiantId: number;
  lien: string;
}

export interface Exercice {
  id: number;
  statut: "SANS_RELECTEUR" | "EN_ATTENTE_RELECTURE" | "EN_COURS_RELECTURE" | "RELU";
}

export interface RelectureResume {
  id: number;
  exerciceId: number;
  lien: string;
  statutExercice: string;
}

export interface RelectureDetail {
  id: number;
  exerciceId: number;
  lien: string;
  ouverteAt: string | null;
  note: number | null;
  commentaire: string | null;
  rendueAt: string | null;
}

export interface RendreRelectureRequete {
  note: number;
  commentaire: string;
}

export interface DetailPresence {
  sessionId: number;
  titre: string;
  present: boolean;
  source: "ETUDIANT" | "FORMATEUR" | null;
}

export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  relecturesEnAttente: number;
  detailPresences: DetailPresence[];
  exercicesEnAttente: number;
}

export interface SessionResume {
  id: number;
  titre: string;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
  clotureAt: string | null;
}

export interface MonExercice {
  id: number;
  sessionId: number;
  titreSession: string;
  lien: string;
  statut: "SANS_RELECTEUR" | "EN_ATTENTE_RELECTURE" | "EN_COURS_RELECTURE" | "RELU";
  note: number | null;
  provisoire: boolean;
  commentaires: string[];
}

export const api = {
  ouvrirSession: (donnees: OuvrirSessionRequete) =>
    requete<SessionOuverte>("POST", "/api/sessions", donnees),
  listerPromotions: () => requete<Promotion[]>("GET", "/api/promotions"),
  listerEtudiantsDeLaPromotion: (promotionId: number) =>
    requete<Etudiant[]>("GET", `/api/promotions/${promotionId}/etudiants`),
  marquerPresence: (donnees: MarquerPresenceRequete) =>
    requete<Presence>("POST", "/api/presences", donnees),
  deposerExercice: (donnees: DeposerExerciceRequete) =>
    requete<Exercice>("POST", "/api/exercices", donnees),
  listerRelecturesAFaire: (etudiantId: number) =>
    requete<RelectureResume[]>("GET", `/api/etudiants/${etudiantId}/relectures`),
  ouvrirRelecture: (relectureId: number) =>
    requete<RelectureDetail>("GET", `/api/relectures/${relectureId}`),
  rendreRelecture: (relectureId: number, donnees: RendreRelectureRequete) =>
    requete<void>("POST", `/api/relectures/${relectureId}`, donnees),
  obtenirTableau: (promotionId: number) =>
    requete<LigneTableau[]>("GET", `/api/tableau?promotionId=${promotionId}`),
  listerSessions: (promotionId: number) =>
    requete<SessionResume[]>("GET", `/api/sessions?promotionId=${promotionId}`),
  ajouterPresenceManuelle: (sessionId: number, etudiantId: number) =>
    requete<Presence>("POST", `/api/sessions/${sessionId}/presences`, { etudiantId }),
  cloturerSession: (sessionId: number) =>
    requete<SessionResume>("POST", `/api/sessions/${sessionId}/cloture`),
  listerMesExercices: (etudiantId: number) =>
    requete<MonExercice[]>("GET", `/api/etudiants/${etudiantId}/exercices`),
  remplacerLien: (exerciceId: number, lien: string) =>
    requete<Exercice>("PUT", `/api/exercices/${exerciceId}`, { lien }),
};
