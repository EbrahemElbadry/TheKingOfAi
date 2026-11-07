import { count, desc, eq } from "drizzle-orm";
import { drizzle } from "drizzle-orm/mysql2";
import { articles, caseStudies, challenges, InsertArticle, InsertCaseStudy, InsertChallenge, InsertUser, users } from "../drizzle/schema";
import { ENV } from "./_core/env";

let _db: ReturnType<typeof drizzle> | null = null;

export async function getDb() {
  if (!_db && process.env.DATABASE_URL) {
    try { _db = drizzle(process.env.DATABASE_URL); }
    catch (error) { console.warn("[Database] Failed to connect:", error); _db = null; }
  }
  return _db;
}

export async function upsertUser(user: InsertUser): Promise<void> {
  if (!user.openId) throw new Error("User openId is required for upsert");
  const db = await getDb();
  if (!db) { console.warn("[Database] Cannot upsert user: database not available"); return; }

  const values: InsertUser = { openId: user.openId, lastSignedIn: user.lastSignedIn ?? new Date() };
  const updateSet: Record<string, unknown> = { lastSignedIn: values.lastSignedIn };
  const textFields = ["name", "email", "loginMethod"] as const;
  for (const field of textFields) {
    if (user[field] !== undefined) { values[field] = user[field] ?? null; updateSet[field] = user[field] ?? null; }
  }
  if (user.role !== undefined) { values.role = user.role; updateSet.role = user.role; }
  else if (user.openId === ENV.ownerOpenId) { values.role = "admin"; updateSet.role = "admin"; }
  await db.insert(users).values(values).onDuplicateKeyUpdate({ set: updateSet });
}

export async function getUserByOpenId(openId: string) {
  const db = await getDb();
  if (!db) return undefined;
  const result = await db.select().from(users).where(eq(users.openId, openId)).limit(1);
  return result[0];
}

export async function createChallenge(input: InsertChallenge) { const db = await getDb(); if (!db) throw new Error("Database unavailable"); await db.insert(challenges).values(input); }
export async function listChallenges() { const db = await getDb(); if (!db) return []; return db.select().from(challenges).orderBy(desc(challenges.createdAt)).limit(100); }
export async function updateChallenge(id: number, status: InsertChallenge["status"], internalNotes?: string) { const db = await getDb(); if (!db) throw new Error("Database unavailable"); await db.update(challenges).set({ status, internalNotes, updatedAt: new Date() }).where(eq(challenges.id, id)); }

export async function getDashboardStats() {
  const db = await getDb();
  if (!db) return { challenges: 0, newChallenges: 0, articles: 0, caseStudies: 0 };
  const [challengeTotal, newChallengeTotal, articleTotal, caseStudyTotal] = await Promise.all([
    db.select({ value: count() }).from(challenges),
    db.select({ value: count() }).from(challenges).where(eq(challenges.status, "new")),
    db.select({ value: count() }).from(articles),
    db.select({ value: count() }).from(caseStudies),
  ]);
  return { challenges: Number(challengeTotal[0]?.value ?? 0), newChallenges: Number(newChallengeTotal[0]?.value ?? 0), articles: Number(articleTotal[0]?.value ?? 0), caseStudies: Number(caseStudyTotal[0]?.value ?? 0) };
}

export async function listPublishedArticles() { const db = await getDb(); if (!db) return []; return db.select().from(articles).where(eq(articles.status, "published")).orderBy(desc(articles.publishedAt)).limit(20); }
export async function getPublishedArticle(slug: string) { const db = await getDb(); if (!db) return undefined; const result = await db.select().from(articles).where(eq(articles.slug, slug)).limit(1); return result[0]?.status === "published" ? result[0] : undefined; }
export async function listPublishedCaseStudies() { const db = await getDb(); if (!db) return []; return db.select().from(caseStudies).where(eq(caseStudies.status, "published")).orderBy(caseStudies.sortOrder, desc(caseStudies.publishedAt)).limit(20); }
export async function listArticles() { const db = await getDb(); if (!db) return []; return db.select().from(articles).orderBy(desc(articles.updatedAt)).limit(100); }
export async function listCaseStudies() { const db = await getDb(); if (!db) return []; return db.select().from(caseStudies).orderBy(desc(caseStudies.updatedAt)).limit(100); }
export async function createArticle(input: InsertArticle) { const db = await getDb(); if (!db) throw new Error("Database unavailable"); await db.insert(articles).values(input); }
export async function createCaseStudy(input: InsertCaseStudy) { const db = await getDb(); if (!db) throw new Error("Database unavailable"); await db.insert(caseStudies).values(input); }
