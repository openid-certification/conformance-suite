/**
 * Calendar constants the plans listing and the statistics page both do their
 * day arithmetic and month labels with.
 * @module lib/calendar
 */

/** Milliseconds in a day; the only date arithmetic either page does is a whole day. */
export const DAY_MS = 86400000;

/** Month names for period chips and axis labels. Fixed rather than `Intl`, so they read the same everywhere. */
export const SHORT_MONTHS = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];
