import { describe, expect, it } from "vitest";
import { appRouter } from "./routers";
import type { TrpcContext } from "./_core/context";

function createContext(user: TrpcContext["user"] = null): TrpcContext {
  return {
    user,
    req: { protocol: "https", headers: {} } as TrpcContext["req"],
    res: {} as TrpcContext["res"],
  };
}

describe("portfolio challenge contract", () => {
  it("rejects an invalid email before reaching persistence", async () => {
    const caller = appRouter.createCaller(createContext());

    await expect(caller.challenge.create({
      name: "Test visitor",
      email: "not-an-email",
      challengeType: "Build",
      brief: "This is a sufficiently detailed problem brief.",
    })).rejects.toMatchObject({ code: "BAD_REQUEST" });
  });

  it("rejects briefs that are too short", async () => {
    const caller = appRouter.createCaller(createContext());

    await expect(caller.challenge.create({
      name: "Test visitor",
      email: "test@example.com",
      challengeType: "Build",
      brief: "Too short",
    })).rejects.toMatchObject({ code: "BAD_REQUEST" });
  });
});

describe("portfolio admin access", () => {
  it("does not expose admin stats to anonymous visitors", async () => {
    const caller = appRouter.createCaller(createContext());
    await expect(caller.admin.stats()).rejects.toMatchObject({ code: "FORBIDDEN" });
  });
});
