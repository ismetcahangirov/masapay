/**
 * The shape a translation must have: every key of the Azerbaijani source, each
 * text a string. Extra keys are allowed for plural forms a language needs and
 * Azerbaijani does not (Russian "_few" / "_many").
 */
export type Messages<T> = {
  [K in keyof T]: T[K] extends string ? string : Messages<T[K]>
} & { [plural: string]: unknown }
