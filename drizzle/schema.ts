import { int, mysqlEnum, mysqlTable, text, timestamp, varchar } from "drizzle-orm/mysql-core";

/**
 * Core user table backing the Manus OAuth flow.
 */
export const users = mysqlTable("users", {
  id: int("id").autoincrement().primaryKey(),
  openId: varchar("openId", { length: 64 }).notNull().unique(),
  name: text("name"),
  email: varchar("email", { length: 320 }),
  loginMethod: varchar("loginMethod", { length: 64 }),
  role: mysqlEnum("role", ["user", "admin"]).default("user").notNull(),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
  lastSignedIn: timestamp("lastSignedIn").defaultNow().notNull(),
});

export const challenges = mysqlTable("challenges", {
  id: int("id").autoincrement().primaryKey(),
  name: varchar("name", { length: 160 }).notNull(),
  email: varchar("email", { length: 320 }).notNull(),
  challengeType: varchar("challengeType", { length: 120 }).notNull(),
  challengeFocus: varchar("challengeFocus", { length: 160 }),
  brief: text("brief").notNull(),
  constraints: text("constraints"),
  timeline: varchar("timeline", { length: 160 }),
  budget: varchar("budget", { length: 160 }),
  tools: text("tools"),
  productUrl: varchar("productUrl", { length: 500 }),
  links: text("links"),
  status: mysqlEnum("status", ["new", "reviewing", "replied", "qualified", "archived"]).default("new").notNull(),
  source: varchar("source", { length: 64 }).default("website").notNull(),
  internalNotes: text("internalNotes"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export const caseStudies = mysqlTable("caseStudies", {
  id: int("id").autoincrement().primaryKey(),
  slug: varchar("slug", { length: 180 }).notNull().unique(),
  title: varchar("title", { length: 220 }).notNull(),
  summary: text("summary").notNull(),
  problem: text("problem").notNull(),
  context: text("context"),
  approach: text("approach"),
  outcome: text("outcome"),
  constraints: text("constraints"),
  coverImageUrl: varchar("coverImageUrl", { length: 500 }),
  externalUrl: varchar("externalUrl", { length: 500 }),
  status: mysqlEnum("status", ["draft", "published"]).default("draft").notNull(),
  sortOrder: int("sortOrder").default(0).notNull(),
  publishedAt: timestamp("publishedAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export const articles = mysqlTable("articles", {
  id: int("id").autoincrement().primaryKey(),
  slug: varchar("slug", { length: 180 }).notNull().unique(),
  title: varchar("title", { length: 220 }).notNull(),
  excerpt: text("excerpt").notNull(),
  content: text("content").notNull(),
  coverImageUrl: varchar("coverImageUrl", { length: 500 }),
  seoTitle: varchar("seoTitle", { length: 220 }),
  seoDescription: varchar("seoDescription", { length: 320 }),
  status: mysqlEnum("status", ["draft", "published"]).default("draft").notNull(),
  readingTime: int("readingTime").default(5).notNull(),
  publishedAt: timestamp("publishedAt"),
  createdAt: timestamp("createdAt").defaultNow().notNull(),
  updatedAt: timestamp("updatedAt").defaultNow().onUpdateNow().notNull(),
});

export type User = typeof users.$inferSelect;
export type InsertUser = typeof users.$inferInsert;
export type Challenge = typeof challenges.$inferSelect;
export type InsertChallenge = typeof challenges.$inferInsert;
export type CaseStudy = typeof caseStudies.$inferSelect;
export type InsertCaseStudy = typeof caseStudies.$inferInsert;
export type Article = typeof articles.$inferSelect;
export type InsertArticle = typeof articles.$inferInsert;
