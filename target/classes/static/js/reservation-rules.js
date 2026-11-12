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

  const isValidInterval = (startDateTime, endDateTime) => {
    if (!startDateTime || !endDateTime) {
      return false;
    }
    const start = new Date(startDateTime);
    const end = new Date(endDateTime);
    return (
      !Number.isNaN(start.getTime()) &&
      !Number.isNaN(end.getTime()) &&
      end > start
    );
  };

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
