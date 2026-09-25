// Format d'erreur imposé par le contrat : { code, message } — traduit ici en une erreur JS unique.
export class ErreurApi extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
    this.name = "ErreurApi";
  }
}
