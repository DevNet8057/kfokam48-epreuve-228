import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { TableauDonnees } from "./TableauDonnees";
import { api } from "../../api/client";

vi.mock("../../api/client", () => ({
  api: { obtenirTableau: vi.fn() },
}));

const ligneAmina = {
  etudiantId: 1,
  nom: "Amina",
  presences: 1,
  exercicesDeposes: 0,
  moyenne: null,
  relecturesEnAttente: 0,
  detailPresences: [],
  exercicesEnAttente: 0,
};

const ligneBoris = { ...ligneAmina, etudiantId: 2, nom: "Boris" };

/**
 * Bug K48-26 : le tableau formateur ne se met pas à jour tant que la promotion n'est pas
 * re-sélectionnée. Reproduit ici : un deuxième étudiant marque sa présence pendant que le
 * formateur regarde l'écran ; sans rafraîchissement automatique, il n'apparaît jamais.
 */
describe("TableauDonnees", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("se met à jour automatiquement sans action du formateur", async () => {
    vi.mocked(api.obtenirTableau)
      .mockResolvedValueOnce([ligneAmina])
      .mockResolvedValueOnce([ligneAmina, ligneBoris]);

    render(<TableauDonnees promotionId={1} />);

    await vi.waitFor(() => expect(screen.getByText("Amina")).toBeInTheDocument());
    expect(screen.queryByText("Boris")).not.toBeInTheDocument();

    // Une nouvelle présence est enregistrée côté serveur pendant que l'écran reste ouvert.
    await vi.advanceTimersByTimeAsync(5000);

    await vi.waitFor(() => expect(screen.getByText("Boris")).toBeInTheDocument());
  });
});
