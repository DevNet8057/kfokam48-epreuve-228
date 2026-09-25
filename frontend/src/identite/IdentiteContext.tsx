import { createContext, ReactNode, useContext, useEffect, useState } from "react";
import { definirEtudiantIdCourant } from "../api/client";

export type Profil = "FORMATEUR" | "ETUDIANT" | "RELECTEUR";

export interface Identite {
  profil: Profil;
  etudiantId?: number;
  nom?: string;
}

const CLE_SESSION = "k48-identite";

function lireIdentiteStockee(): Identite | null {
  try {
    const brut = sessionStorage.getItem(CLE_SESSION);
    return brut ? (JSON.parse(brut) as Identite) : null;
  } catch {
    // Stockage indisponible (navigation privée, quota…) : pas d'identité restaurée.
    return null;
  }
}

interface IdentiteContextValue {
  identite: Identite | null;
  seConnecter: (identite: Identite) => void;
  changerIdentite: () => void;
}

const IdentiteContext = createContext<IdentiteContextValue | undefined>(undefined);

/**
 * K48-25 : identité conservée jusqu'à la fermeture de l'onglet (sessionStorage, pas localStorage).
 */
export function IdentiteProvider({ children }: { children: ReactNode }) {
  const [identite, setIdentite] = useState<Identite | null>(() => lireIdentiteStockee());

  useEffect(() => {
    definirEtudiantIdCourant(identite?.etudiantId);
    try {
      if (identite) {
        sessionStorage.setItem(CLE_SESSION, JSON.stringify(identite));
      } else {
        sessionStorage.removeItem(CLE_SESSION);
      }
    } catch {
      // Le stockage échoue silencieusement : l'identité reste valide pour l'onglet en cours.
    }
  }, [identite]);

  return (
    <IdentiteContext.Provider value={{ identite, seConnecter: setIdentite, changerIdentite: () => setIdentite(null) }}>
      {children}
    </IdentiteContext.Provider>
  );
}

export function useIdentite(): IdentiteContextValue {
  const contexte = useContext(IdentiteContext);
  if (!contexte) {
    throw new Error("useIdentite doit être utilisé à l'intérieur d'un IdentiteProvider.");
  }
  return contexte;
}
