package az.masapay.pos.iiko;

/** Body of POST /api/1/access_token: exchanges the restaurant's API login for a token. */
public record IikoTokenRequest(String apiLogin) {}
