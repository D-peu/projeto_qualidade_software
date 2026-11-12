"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const projectRoot = path.resolve(__dirname, "..");
const rules = require(path.join(
  projectRoot,
  "src/main/resources/static/js/reservation-rules.js",
));

const readProjectFile = (relativePath) =>
  fs.readFileSync(path.join(projectRoot, relativePath), "utf8");

const enumValues = (relativePath, enumName) => {
  const source = readProjectFile(relativePath);
  const match = source.match(new RegExp(`enum\\s+${enumName}\\s*\\{([^}]*)\\}`));
  assert.ok(match, `Could not find ${enumName} in ${relativePath}`);
  return match[1]
    .split(",")
    .map((value) => value.trim())
    .filter(Boolean);
};

test("frontend room states stay synchronized with the Spring Boot enum", () => {
  assert.deepEqual(
    rules.ROOM_STATUSES,
    enumValues(
      "src/main/java/br/com/projeto_qualidade_software/projeto/enums/StatusSala.java",
      "StatusSala",
    ),
  );
});

test("frontend reservation states stay synchronized with the Spring Boot enum", () => {
  assert.deepEqual(
    rules.RESERVATION_STATUSES,
    enumValues(
      "src/main/java/br/com/projeto_qualidade_software/projeto/enums/StatusReserva.java",
      "StatusReserva",
    ),
  );
});

test("only available rooms accept new requests", () => {
  for (const status of rules.ROOM_STATUSES) {
    assert.equal(
      rules.canRequestRoom({ statusSala: status }),
      status === rules.ROOM_STATUS.AVAILABLE,
    );
  }
  assert.equal(rules.canRequestRoom(null), false);
});

test("responsible can decide only requested reservations", () => {
  const reservation = { status: rules.RESERVATION_STATUS.REQUESTED };
  assert.equal(
    rules.decideReservation(reservation, rules.RESERVATION_STATUS.APPROVED, {
      statusSala: rules.ROOM_STATUS.RESERVED,
    }),
    true,
  );
  assert.equal(reservation.status, rules.RESERVATION_STATUS.APPROVED);
  assert.equal(
    rules.decideReservation(reservation, rules.RESERVATION_STATUS.REJECTED, {
      statusSala: rules.ROOM_STATUS.AVAILABLE,
    }),
    false,
  );
});

test("responsible cannot approve a request for a room under maintenance", () => {
  const reservation = { status: rules.RESERVATION_STATUS.REQUESTED };
  assert.equal(
    rules.decideReservation(reservation, rules.RESERVATION_STATUS.APPROVED, {
      statusSala: rules.ROOM_STATUS.MAINTENANCE,
    }),
    false,
  );
  assert.equal(reservation.status, rules.RESERVATION_STATUS.REQUESTED);
  assert.equal(
    rules.decideReservation(reservation, rules.RESERVATION_STATUS.REJECTED, {
      statusSala: rules.ROOM_STATUS.MAINTENANCE,
    }),
    true,
  );
});

test("cancellation is limited to requested or approved reservations", () => {
  const requested = { status: rules.RESERVATION_STATUS.REQUESTED };
  const rejected = { status: rules.RESERVATION_STATUS.REJECTED };
  assert.equal(rules.cancelReservation(requested), true);
  assert.equal(requested.status, rules.RESERVATION_STATUS.CANCELLED);
  assert.equal(rules.cancelReservation(rejected), false);
});

test("reservation intervals require a valid end after the start", () => {
  const now = new Date("2040-01-01T13:00:00Z");
  assert.equal(
    rules.isValidInterval("2040-01-02T09:00", "2040-01-02T10:00", now),
    true,
  );
  assert.equal(
    rules.isValidInterval("2040-01-02T10:00", "2040-01-02T10:00", now),
    false,
  );
  assert.equal(
    rules.isValidInterval("2040-01-02T11:00", "2040-01-02T10:00", now),
    false,
  );
  assert.equal(rules.isValidInterval("", "2040-01-02T10:00", now), false);
});

test("CP-101–116 / RF07 / RN01 accepts future half-hour intervals and explains invalid input", () => {
  const now = new Date("2040-01-01T13:00:00Z");
  const invalidIntervals = [
    ["data passada", "2039-12-31T10:00", "2039-12-31T11:00", "futuro"],
    ["exatamente agora", "2040-01-01T10:00", "2040-01-01T11:00", "futuro"],
    [
      "um minuto no futuro",
      "2040-01-01T10:01",
      "2040-01-01T11:01",
      "30 minutos",
    ],
    ["início no minuto 15", "2040-01-02T10:15", "2040-01-02T11:15", "30 minutos"],
    ["início no minuto 45", "2040-01-02T10:45", "2040-01-02T11:45", "30 minutos"],
    ["fim no minuto 15", "2040-01-02T10:00", "2040-01-02T11:15", "30 minutos"],
    ["fim no minuto 45", "2040-01-02T10:00", "2040-01-02T11:45", "30 minutos"],
    [
      "segundos diferentes de zero",
      "2040-01-02T10:00:01",
      "2040-01-02T11:00",
      "segundos e nanos",
    ],
    [
      "nanos diferentes de zero",
      "2040-01-02T10:00:00.000000001",
      "2040-01-02T11:00",
      "segundos e nanos",
    ],
    [
      "fim igual ao início",
      "2040-01-02T10:00",
      "2040-01-02T10:00",
      "posterior",
    ],
    [
      "fim anterior ao início",
      "2040-01-02T11:00",
      "2040-01-02T10:00",
      "posterior",
    ],
  ];

  for (const [caso, inicio, fim, mensagem] of invalidIntervals) {
    assert.equal(rules.isValidInterval(inicio, fim, now), false, caso);
    assert.match(rules.getIntervalErrorMessage(inicio, fim, now), new RegExp(mensagem));
  }

  assert.equal(
    rules.isValidInterval("2040-01-02T10:00", "2040-01-02T11:00", now),
    true,
  );
  assert.equal(
    rules.isValidInterval("2040-01-02T10:30", "2040-01-02T12:00", now),
    true,
  );
});

test("CP-117–119 / RF07 / RN01 compares reservation times in São Paulo regardless of browser timezone", () => {
  const originalTimezone = process.env.TZ;
  process.env.TZ = "UTC";
  try {
    const nowAt2350SaoPaulo = new Date("2026-11-13T02:50:00Z");
    assert.equal(
      rules.isValidInterval(
        "2026-11-12T23:30",
        "2026-11-13T00:00",
        nowAt2350SaoPaulo,
      ),
      false,
      "23:30 do mesmo dia já passou em São Paulo",
    );
    assert.equal(
      rules.isValidInterval(
        "2026-11-13T00:00",
        "2026-11-13T01:00",
        nowAt2350SaoPaulo,
      ),
      true,
      "00:00 do dia seguinte ainda está no futuro em São Paulo",
    );

    assert.equal(
      rules.isValidInterval(
        "2026-11-12T00:30",
        "2026-11-12T01:30",
        new Date("2026-11-12T04:00:00Z"),
      ),
      false,
      "00:30 já passou às 01:00 em São Paulo",
    );
  } finally {
    if (originalTimezone === undefined) {
      delete process.env.TZ;
    } else {
      process.env.TZ = originalTimezone;
    }
  }
});

test("schedule conflicts reject overlap but allow adjacent reservations", () => {
  const reservations = [
    {
      id: "RSV-1",
      salaId: 101,
      inicio: "2040-10-16T09:00",
      fim: "2040-10-16T10:00",
      status: rules.RESERVATION_STATUS.APPROVED,
    },
    {
      id: "RSV-2",
      salaId: 101,
      inicio: "2040-10-16T12:00",
      fim: "2040-10-16T13:00",
      status: rules.RESERVATION_STATUS.CANCELLED,
    },
  ];

  assert.equal(
    rules.hasScheduleConflict(
      reservations,
      101,
      "2040-10-16T09:30",
      "2040-10-16T10:30",
    ),
    true,
  );
  assert.equal(
    rules.hasScheduleConflict(
      reservations,
      101,
      "2040-10-16T10:00",
      "2040-10-16T11:00",
    ),
    false,
  );
  assert.equal(
    rules.hasScheduleConflict(
      reservations,
      204,
      "2040-10-16T09:30",
      "2040-10-16T10:30",
    ),
    false,
  );
  assert.equal(
    rules.hasScheduleConflict(
      reservations,
      101,
      "2040-10-16T09:30",
      "2040-10-16T10:30",
      "RSV-1",
    ),
    false,
  );
});

test("room maintenance stays blocked while other room states follow active reservations", () => {
  const activeRequest = {
    salaId: 101,
    status: rules.RESERVATION_STATUS.REQUESTED,
  };
  const cancelledRequest = {
    salaId: 204,
    status: rules.RESERVATION_STATUS.CANCELLED,
  };

  assert.equal(
    rules.getRoomAvailability(
      rules.ROOM_STATUS.MAINTENANCE,
      [activeRequest],
      101,
    ),
    rules.ROOM_STATUS.MAINTENANCE,
  );
  assert.equal(
    rules.getRoomAvailability(
      rules.ROOM_STATUS.AVAILABLE,
      [activeRequest],
      101,
    ),
    rules.ROOM_STATUS.RESERVED,
  );
  assert.equal(
    rules.getRoomAvailability(
      rules.ROOM_STATUS.RESERVED,
      [cancelledRequest],
      204,
    ),
    rules.ROOM_STATUS.AVAILABLE,
  );
});

test("Spring Boot static entry point loads the frontend and reservation API client", () => {
  const html = readProjectFile("src/main/resources/static/index.html");
  const app = readProjectFile("src/main/resources/static/js/app.js");
  const styles = readProjectFile("src/main/resources/static/css/app.css");

  assert.match(html, /css\/app\.css/);
  assert.match(html, /js\/reservation-rules\.js/);
  assert.match(html, /js\/reservation-api\.js/);
  assert.match(html, /js\/app\.js/);
  assert.match(html, /data-profile="solicitante"/);
  assert.match(html, /data-profile="responsavel"/);
  assert.match(html, /data-profile="administrador"/);
  assert.match(app, /ReservationApi\.login/);
  assert.match(app, /ReservationApi\.currentUser/);
  assert.match(app, /ReservationApi\.logout/);
  assert.doesNotMatch(app, /mockUsers|localStorage/);
  assert.match(app, /EM_MANUTENCAO/);
  assert.match(app, /approve-reservation/);
  assert.match(app, /reject-reservation/);
  assert.match(app, /edit-reservation/);
  assert.match(app, /cancel-reservation/);
  assert.match(app, /ReservationApi\.listRooms/);
  assert.match(app, /ReservationApi\.createReservation/);
  assert.match(app, /ReservationApi\.changeSchedule/);
  assert.match(app, /ReservationApi\.cancelReservation/);
  assert.match(app, /ReservationApi\.decideReservation/);
  assert.doesNotMatch(app, /Marina Alves|RSV-204/);
  assert.match(styles, /@media \(max-width: 760px\)/);
});

test("backend exposes the read endpoints consumed by the frontend", () => {
  const salaController = readProjectFile(
    "src/main/java/br/com/projeto_qualidade_software/projeto/api/SalaController.java",
  );
  const reservaController = readProjectFile(
    "src/main/java/br/com/projeto_qualidade_software/projeto/api/ReservaController.java",
  );

  assert.match(salaController, /@RequestMapping\("\/api\/salas"\)/);
  assert.match(salaController, /@GetMapping/);
  assert.match(salaController, /salaService\.listarSalas\(\)/);
  assert.match(reservaController, /@RequestParam\("status"\) StatusReserva status/);
  assert.match(reservaController, /listarReservas\(status\)/);
});
