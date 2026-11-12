((root) => {
  "use strict";

  const ROOM_STATUS = Object.freeze({
    AVAILABLE: "DISPONIVEL",
    RESERVED: "RESERVADA",
    MAINTENANCE: "EM_MANUTENCAO",
  });

  const RESERVATION_STATUS = Object.freeze({
    REQUESTED: "SOLICITADA",
    APPROVED: "APROVADA",
    IN_USE: "EM_USO",
    COMPLETED: "CONCLUIDA",
    REJECTED: "REJEITADA",
    CANCELLED: "CANCELADA",
    NO_SHOW: "NAO_COMPARECEU",
  });

  const ROOM_STATUSES = Object.values(ROOM_STATUS);
  const RESERVATION_STATUSES = Object.values(RESERVATION_STATUS);
  const ACTIVE_RESERVATION_STATUSES = new Set([
    RESERVATION_STATUS.REQUESTED,
    RESERVATION_STATUS.APPROVED,
    RESERVATION_STATUS.IN_USE,
  ]);
  const isRoomStatus = (status) => ROOM_STATUSES.includes(status);

  const parseIntervalDateTime = (value) => {
    const match =
      /^(\d{4}-\d{2}-\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?$/.exec(
        value || "",
      );
    if (!match) {
      return null;
    }
    const seconds = Number(match[4] || 0);
    const fraction = match[5] || "";
    const date = new Date(
      `${match[1]}T${match[2]}:${match[3]}:${String(seconds).padStart(2, "0")}Z`,
    );
    if (
      Number.isNaN(date.getTime()) ||
      date.toISOString().slice(0, 19) !==
        `${match[1]}T${match[2]}:${match[3]}:${String(seconds).padStart(2, "0")}`
    ) {
      return null;
    }
    return {
      date,
      minute: Number(match[3]),
      seconds,
      nanosZero: !/[1-9]/.test(fraction),
    };
  };

  const getIntervalErrorMessage = (
    startDateTime,
    endDateTime,
    now = new Date(),
  ) => {
    const start = parseIntervalDateTime(startDateTime);
    const end = parseIntervalDateTime(endDateTime);
    if (!start || !end) {
      return "Informe datas e horários válidos.";
    }
    const saoPauloParts = new Intl.DateTimeFormat("en-CA", {
      timeZone: "America/Sao_Paulo",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    })
      .formatToParts(now)
      .reduce((parts, part) => {
        if (part.type !== "literal") {
          parts[part.type] = part.value;
        }
        return parts;
      }, {});
    const saoPauloNow = new Date(
      `${saoPauloParts.year}-${saoPauloParts.month}-${saoPauloParts.day}T${saoPauloParts.hour}:${saoPauloParts.minute}:00Z`,
    );
    if (start.date <= saoPauloNow) {
      return "A data e hora de início devem estar no futuro.";
    }
    if (start.minute % 30 !== 0 || end.minute % 30 !== 0) {
      return "O início e o término devem estar em incrementos de 30 minutos (minutos 00 ou 30).";
    }
    if (
      start.seconds !== 0 ||
      end.seconds !== 0 ||
      !start.nanosZero ||
      !end.nanosZero
    ) {
      return "Os segundos e nanos devem ser zerados.";
    }
    if (end.date <= start.date) {
      return "O término deve ser posterior ao início.";
    }
    return "";
  };

  const isValidInterval = (startDateTime, endDateTime, now = new Date()) =>
    getIntervalErrorMessage(startDateTime, endDateTime, now) === "";

  const canRequestRoom = (room) =>
    Boolean(room && room.statusSala === ROOM_STATUS.AVAILABLE);

  const decideReservation = (reservation, decision, room) => {
    if (
      !reservation ||
      reservation.status !== RESERVATION_STATUS.REQUESTED ||
      ![RESERVATION_STATUS.APPROVED, RESERVATION_STATUS.REJECTED].includes(
        decision,
      ) ||
      (decision === RESERVATION_STATUS.APPROVED &&
        (!room || room.statusSala === ROOM_STATUS.MAINTENANCE))
    ) {
      return false;
    }
    reservation.status = decision;
    return true;
  };

  const cancelReservation = (reservation) => {
    if (
      !reservation ||
      ![RESERVATION_STATUS.REQUESTED, RESERVATION_STATUS.APPROVED].includes(
        reservation.status,
      )
    ) {
      return false;
    }
    reservation.status = RESERVATION_STATUS.CANCELLED;
    return true;
  };

  const hasScheduleConflict = (
    reservations,
    roomId,
    startDateTime,
    endDateTime,
    excludedReservationId = null,
  ) => {
    const start = new Date(startDateTime);
    const end = new Date(endDateTime);
    if (
      Number.isNaN(start.getTime()) ||
      Number.isNaN(end.getTime()) ||
      end <= start
    ) {
      return true;
    }

    return reservations.some((reservation) => {
      if (
        reservation.salaId !== roomId ||
        reservation.id === excludedReservationId ||
        !ACTIVE_RESERVATION_STATUSES.has(reservation.status)
      ) {
        return false;
      }
      const existingStart = new Date(reservation.inicio);
      const existingEnd = new Date(reservation.fim);
      return (
        !Number.isNaN(existingStart.getTime()) &&
        !Number.isNaN(existingEnd.getTime()) &&
        start < existingEnd &&
        end > existingStart
      );
    });
  };

  const getRoomAvailability = (currentRoomStatus, reservations, roomId) => {
    if (currentRoomStatus === ROOM_STATUS.MAINTENANCE) {
      return ROOM_STATUS.MAINTENANCE;
    }
    return reservations.some(
      (reservation) =>
        reservation.salaId === roomId &&
        ACTIVE_RESERVATION_STATUSES.has(reservation.status),
    )
      ? ROOM_STATUS.RESERVED
      : ROOM_STATUS.AVAILABLE;
  };

  const rules = Object.freeze({
    ROOM_STATUS,
    RESERVATION_STATUS,
    ROOM_STATUSES,
    RESERVATION_STATUSES,
    ACTIVE_RESERVATION_STATUSES,
    isRoomStatus,
    isValidInterval,
    getIntervalErrorMessage,
    canRequestRoom,
    decideReservation,
    cancelReservation,
    hasScheduleConflict,
    getRoomAvailability,
  });

  if (typeof module !== "undefined" && module.exports) {
    module.exports = rules;
  } else {
    root.ReservationRules = rules;
  }
})(typeof globalThis === "undefined" ? this : globalThis);
