import { z } from "zod";
import { COOKIE_NAME } from "@shared/const";
import { getSessionCookieOptions } from "./_core/cookies";
import { systemRouter } from "./_core/systemRouter";
import { adminProcedure, publicProcedure, router } from "./_core/trpc";
import {
  createArticle,
  createCaseStudy,
  createChallenge,
  getDashboardStats,
  getPublishedArticle,
  listArticles,
  listCaseStudies,
  listChallenges,
  listPublishedArticles,
  listPublishedCaseStudies,
  updateChallenge,
} from "./db";

const challengeInput = z.object({
  name: z.string().trim().min(2).max(160),
  email: z.string().trim().email().max(320),
  challengeType: z.string().trim().min(1).max(120),
  challengeFocus: z.string().trim().max(160).optional(),
  brief: z.string().trim().min(12).max(8000),
  constraints: z.string().trim().max(2000).optional(),
  timeline: z.string().trim().max(160).optional(),
  budget: z.string().trim().max(160).optional(),
  tools: z.string().trim().max(2000).optional(),
  productUrl: z.string().trim().url().max(500).optional().or(z.literal("")),
  links: z.string().trim().max(2000).optional(),
});

const publishStatus = z.enum(["draft", "published"]);
const challengeStatus = z.enum(["new", "reviewing", "replied", "qualified", "archived"]);

export const appRouter = router({
  system: systemRouter,
  auth: router({
    me: publicProcedure.query((opts) => opts.ctx.user),
    logout: publicProcedure.mutation(({ ctx }) => {
      const cookieOptions = getSessionCookieOptions(ctx.req);
      ctx.res.clearCookie(COOKIE_NAME, { ...cookieOptions, maxAge: -1 });
      return { success: true } as const;
    }),
  }),

  challenge: router({
    create: publicProcedure.input(challengeInput).mutation(async ({ input }) => {
      await createChallenge({ ...input, source: "website" });
      return { success: true } as const;
    }),
  }),

  portfolio: router({
    articleBySlug: publicProcedure.input(z.object({ slug: z.string().min(1).max(180) })).query(({ input }) => getPublishedArticle(input.slug)),
    publishedContent: publicProcedure.query(async () => ({
      caseStudies: await listPublishedCaseStudies(),
      articles: await listPublishedArticles(),
    })),
  }),

  admin: router({
    stats: adminProcedure.query(() => getDashboardStats()),
    challenges: adminProcedure.query(() => listChallenges()),
    updateChallenge: adminProcedure
      .input(z.object({ id: z.number().int().positive(), status: challengeStatus, internalNotes: z.string().max(5000).optional() }))
      .mutation(async ({ input }) => {
        await updateChallenge(input.id, input.status, input.internalNotes);
        return { success: true } as const;
      }),
    articles: adminProcedure.query(() => listArticles()),
    caseStudies: adminProcedure.query(() => listCaseStudies()),
    createArticle: adminProcedure
      .input(z.object({
        slug: z.string().trim().min(2).max(180),
        title: z.string().trim().min(2).max(220),
        excerpt: z.string().trim().min(10).max(1000),
        content: z.string().trim().min(20),
        status: publishStatus.default("draft"),
        readingTime: z.number().int().min(1).max(120).default(5),
      }))
      .mutation(async ({ input }) => {
        await createArticle({ ...input, publishedAt: input.status === "published" ? new Date() : null });
        return { success: true } as const;
      }),
    createCaseStudy: adminProcedure
      .input(z.object({
        slug: z.string().trim().min(2).max(180),
        title: z.string().trim().min(2).max(220),
        summary: z.string().trim().min(10).max(1000),
        problem: z.string().trim().min(10),
        outcome: z.string().trim().optional(),
        status: publishStatus.default("draft"),
      }))
      .mutation(async ({ input }) => {
        await createCaseStudy({ ...input, publishedAt: input.status === "published" ? new Date() : null });
        return { success: true } as const;
      }),
  }),
});

export type AppRouter = typeof appRouter;
