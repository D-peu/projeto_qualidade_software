((root) => {
  "use strict";

  class ApiError extends Error {
    constructor(message, status = 0) {
      super(message);
      this.name = "ApiError";
      this.status = status;
    }
  }

  const createApi = (fetchImpl = root.fetch.bind(root), baseUrl = "") => {
    let csrfToken = "";
    const request = async (path, options = {}) => {
      let response;
      try {
        response = await fetchImpl(`${baseUrl}${path}`, {
          ...options,
          credentials: "same-origin",
          headers: {
            Accept: "application/json",
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(csrfToken && !["GET", "HEAD"].includes(options.method || "GET")
              ? { "X-CSRF-Token": csrfToken }
              : {}),
            ...options.headers,
          },
        });
      } catch (error) {
        throw new ApiError(
          "Não foi possível conectar à API. Verifique se o servidor está em execução e tente novamente.",
        );
      }

      const responseText = await response.text();
      let data = null;
      if (responseText) {
        try {
          data = JSON.parse(responseText);
        } catch (error) {
          if (response.ok) {
            throw new ApiError("A API retornou uma resposta inválida.", response.status);
          }
        }
      }

      if (!response.ok) {
        throw new ApiError(
          data?.mensagem || `A API respondeu com erro HTTP ${response.status}.`,
          response.status,
        );
      }

      if (responseText && data === null) {
        throw new ApiError("A API retornou uma resposta inválida.", response.status);
      }
      return data;
    };

    const ensureCsrfToken = async () => {
      if (!csrfToken) {
        const data = await request("/api/auth/csrf");
        if (typeof data?.csrfToken !== "string" || !data.csrfToken) {
          throw new ApiError("A API não forneceu o token de segurança.");
        }
        csrfToken = data.csrfToken;
      }
    };

    return Object.freeze({
      ApiError,
      createApi,
      login: async (email, senha) => {
        await ensureCsrfToken();
        const data = await request("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({ email, senha }),
        });
        csrfToken = data.csrfToken;
        return data;
      },
      currentUser: async () => {
        await ensureCsrfToken();
        const data = await request("/api/auth/me");
        csrfToken = data.csrfToken;
        return data;
      },
      logout: async () => {
        try {
          await request("/api/auth/logout", { method: "POST" });
        } finally {
          csrfToken = "";
        }
      },
      listRooms: () => request("/api/salas"),
      listProfessors: () => request("/api/professores"),
      listMaterials: () => request("/api/materiais"),
      listReservations: (status) => {
        if (!status) {
          throw new ApiError("Informe o estado para listar reservas.");
        }
        return request(`/api/reservas?status=${encodeURIComponent(status)}`);
      },
      listApplicantReservations: (applicantId) =>
        request(`/api/reservas/solicitante/${encodeURIComponent(applicantId)}`),
      createReservation: (payload) =>
        request("/api/reservas", {
          method: "POST",
          body: JSON.stringify(payload),
        }),
      changeSchedule: (reservationId, payload) =>
        request(`/api/reservas/${encodeURIComponent(reservationId)}/horario`, {
          method: "PUT",
          body: JSON.stringify(payload),
        }),
      cancelReservation: (reservationId, usuarioId) =>
        request(`/api/reservas/${encodeURIComponent(reservationId)}/cancelamento`, {
          method: "POST",
          body: JSON.stringify({ usuarioId }),
        }),
      decideReservation: (reservationId, usuarioId, tipoAprovador, decision) =>
        request(
          `/api/reservas/${encodeURIComponent(reservationId)}/${decision}`,
          {
            method: "POST",
            body: JSON.stringify({ usuarioId, tipoAprovador }),
          },
        ),
    });
  };

  const api = createApi();

  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  } else {
    root.ReservationApi = api;
  }
})(typeof globalThis === "undefined" ? this : globalThis);
