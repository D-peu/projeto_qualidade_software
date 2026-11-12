"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { ApiError, createApi } = require("../src/main/resources/static/js/reservation-api.js");

const response = (status, body) => ({
  ok: status >= 200 && status < 300,
  status,
  text: async () => (body === undefined ? "" : JSON.stringify(body)),
});

test("authenticates with the server session and sends CSRF tokens on mutations", async () => {
  const calls = [];
  const api = createApi(async (url, options) => {
    calls.push({ url, options });
    if (url === "/api/auth/csrf") {
      return response(200, { csrfToken: "anonymous-token" });
    }
    if (url === "/api/auth/login") {
      return response(200, {
        id: 7,
        nome: "Professor Teste",
        email: "professor@teste.local",
        perfil: "SOLICITANTE",
        tipoSolicitante: "PROFESSOR",
        csrfToken: "authenticated-token",
      });
    }
    if (url === "/api/reservas") {
      return response(201, { id: 9 });
    }
    return response(200, { csrfToken: "restored-token" });
  });

  const user = await api.login("professor@teste.local", "Professor@123");
  await api.createReservation({
    salaId: 1,
    solicitanteId: user.id,
    tipoSolicitante: user.tipoSolicitante,
    inicio: "2026-11-06T09:00",
    fim: "2026-11-06T10:00",
    professorIds: [9],
    materialIds: [31, 32],
  });

  assert.equal(calls[0].url, "/api/auth/csrf");
  assert.equal(calls[1].url, "/api/auth/login");
  assert.deepEqual(JSON.parse(calls[1].options.body), {
    email: "professor@teste.local",
    senha: "Professor@123",
  });
  assert.equal(calls[1].options.headers["X-CSRF-Token"], "anonymous-token");
  assert.equal(calls[1].options.credentials, "same-origin");
  assert.equal(calls[2].options.headers["X-CSRF-Token"], "authenticated-token");
  assert.deepEqual(JSON.parse(calls[2].options.body), {
    salaId: 1,
    solicitanteId: user.id,
    tipoSolicitante: user.tipoSolicitante,
    inicio: "2026-11-06T09:00",
    fim: "2026-11-06T10:00",
    professorIds: [9],
    materialIds: [31, 32],
  });
});

test("reads rooms and filtered reservations from the backend", async () => {
  const calls = [];
  const api = createApi(async (url, options) => {
    calls.push({ url, options });
    return response(200, []);
  });

  await api.listRooms();
  await api.listProfessors();
  await api.listMaterials();
  await api.listReservations("SOLICITADA");
  await api.listApplicantReservations(7);

  assert.equal(calls[0].url, "/api/salas");
  assert.equal(calls[0].options.headers.Accept, "application/json");
  assert.equal(calls[1].url, "/api/professores");
  assert.equal(calls[2].url, "/api/materiais");
  assert.equal(calls[3].url, "/api/reservas?status=SOLICITADA");
  assert.equal(calls[4].url, "/api/reservas/solicitante/7");
  assert.throws(() => api.listReservations(""), /Informe o estado/);
});

test("sends reservation mutations using the existing backend contract", async () => {
  const calls = [];
  const api = createApi(async (url, options) => {
    calls.push({ url, options });
    return response(200, { id: 8 });
  });

  await api.createReservation({
    salaId: 3,
    solicitanteId: 9,
    tipoSolicitante: "PROFESSOR",
    inicio: "2026-11-06T09:00",
    fim: "2026-11-06T10:00",
    professorIds: [9],
    materialIds: [31, 32],
  });
  await api.changeSchedule(8, {
    solicitanteId: 9,
    inicio: "2026-11-06T10:00",
    fim: "2026-11-06T11:00",
  });
  await api.cancelReservation(8, 9);
  await api.decideReservation(8, 4, "RESPONSAVEL", "aprovacao");
  await api.decideReservation(8, 5, "ADMINISTRADOR", "rejeicao");

  assert.deepEqual(
    calls.map(({ url, options }) => [url, options.method]),
    [
      ["/api/reservas", "POST"],
      ["/api/reservas/8/horario", "PUT"],
      ["/api/reservas/8/cancelamento", "POST"],
      ["/api/reservas/8/aprovacao", "POST"],
      ["/api/reservas/8/rejeicao", "POST"],
    ],
  );
  assert.deepEqual(JSON.parse(calls[2].options.body), { usuarioId: 9 });
  assert.deepEqual(JSON.parse(calls[3].options.body), {
    usuarioId: 4,
    tipoAprovador: "RESPONSAVEL",
  });
  assert.deepEqual(JSON.parse(calls[4].options.body), {
    usuarioId: 5,
    tipoAprovador: "ADMINISTRADOR",
  });
});

test("reports backend validation errors without hiding their message", async () => {
  const api = createApi(async () =>
    response(403, { mensagem: "Solicitante sem permissão." }),
  );

  await assert.rejects(api.cancelReservation(8, 99), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 403);
    assert.equal(error.message, "Solicitante sem permissão.");
    return true;
  });
});

test("reports network and malformed successful responses", async () => {
  const networkApi = createApi(async () => {
    throw new Error("offline");
  });
  await assert.rejects(networkApi.listRooms(), /Não foi possível conectar à API/);

  const invalidApi = createApi(async () => ({
    ok: true,
    status: 200,
    text: async () => "not-json",
  }));
  await assert.rejects(invalidApi.listRooms(), /resposta inválida/);
});
