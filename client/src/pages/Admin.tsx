import { FormEvent, useEffect, useState } from "react";
import { ArrowUpRight, BriefcaseBusiness, FileText, Inbox, LayoutDashboard } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/_core/hooks/useAuth";
import DashboardLayout from "@/components/DashboardLayout";
import { trpc } from "@/lib/trpc";
import "./admin.css";
import { useLocation } from "wouter";

type AdminTab = "overview" | "challenges" | "content";

const dateLabel = (value: Date | string | null | undefined) => value ? new Date(value).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" }) : "—";

function Metric({ label, value }: { label: string; value: number | undefined }) {
  return <div className="admin-stat"><span>{label}</span><strong>{value ?? "—"}</strong></div>;
}

function ArticleForm({ onCreated }: { onCreated: () => void }) {
  const mutation = trpc.admin.createArticle.useMutation({
    onSuccess: () => { toast.success("Article saved as a draft."); onCreated(); },
    onError: (error) => toast.error(error.message),
  });

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    mutation.mutate({
      slug: String(data.get("slug") || ""),
      title: String(data.get("title") || ""),
      excerpt: String(data.get("excerpt") || ""),
      content: String(data.get("content") || ""),
      status: String(data.get("status") || "draft") as "draft" | "published",
      readingTime: Number(data.get("readingTime") || 5),
    });
    event.currentTarget.reset();
  };

  return <form className="admin-form" onSubmit={submit}>
    <label>Slug<input name="slug" required placeholder="reliable-ai-systems" /></label>
    <label>Title<input name="title" required placeholder="A real article title" /></label>
    <label>Excerpt<textarea name="excerpt" required placeholder="Short editorial summary" /></label>
    <label>Content<textarea name="content" required placeholder="Markdown-ready content" /></label>
    <label>Reading time<input name="readingTime" type="number" min="1" max="120" defaultValue="5" /></label>
    <label>Visibility<select name="status" defaultValue="draft"><option value="draft">Draft — keep private</option><option value="published">Published — show publicly</option></select></label>
    <button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Saving…" : "Save draft"}</button>
  </form>;
}

function CaseStudyForm({ onCreated }: { onCreated: () => void }) {
  const mutation = trpc.admin.createCaseStudy.useMutation({
    onSuccess: () => { toast.success("Case study saved as a draft."); onCreated(); },
    onError: (error) => toast.error(error.message),
  });

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    mutation.mutate({
      slug: String(data.get("slug") || ""),
      title: String(data.get("title") || ""),
      summary: String(data.get("summary") || ""),
      problem: String(data.get("problem") || ""),
      outcome: String(data.get("outcome") || ""),
      status: String(data.get("status") || "draft") as "draft" | "published",
    });
    event.currentTarget.reset();
  };

  return <form className="admin-form" onSubmit={submit}>
    <label>Slug<input name="slug" required placeholder="client-safe-outcome" /></label>
    <label>Title<input name="title" required placeholder="A verified case study" /></label>
    <label>Summary<textarea name="summary" required placeholder="Concise public summary" /></label>
    <label>Problem<textarea name="problem" required placeholder="The actual problem and constraints" /></label>
    <label>Outcome<textarea name="outcome" placeholder="Verified result, when approved" /></label>
    <label>Visibility<select name="status" defaultValue="draft"><option value="draft">Draft — keep private</option><option value="published">Published — show publicly</option></select></label>
    <button type="submit" disabled={mutation.isPending}>{mutation.isPending ? "Saving…" : "Save draft"}</button>
  </form>;
}

function AdminContent() {
  const stats = trpc.admin.stats.useQuery(undefined, { retry: false });
  const challenges = trpc.admin.challenges.useQuery(undefined, { retry: false });
  const articles = trpc.admin.articles.useQuery(undefined, { retry: false });
  const caseStudies = trpc.admin.caseStudies.useQuery(undefined, { retry: false });
  const utils = trpc.useUtils();
  const [location] = useLocation();
  const [tab, setTab] = useState<AdminTab>("overview");

  useEffect(() => {
    const requestedTab = new URLSearchParams(location.split("?")[1] ?? "").get("tab");
    if (requestedTab === "challenges" || requestedTab === "content") setTab(requestedTab);
    if (!requestedTab) setTab("overview");
  }, [location]);

  const updateChallenge = trpc.admin.updateChallenge.useMutation({
    onSuccess: () => { toast.success("Challenge status updated."); void utils.admin.challenges.invalidate(); void utils.admin.stats.invalidate(); },
    onError: (error) => toast.error(error.message),
  });

  const refreshContent = () => { void utils.admin.articles.invalidate(); void utils.admin.caseStudies.invalidate(); void utils.admin.stats.invalidate(); };
  const accessError = stats.error?.data?.code === "FORBIDDEN" || challenges.error?.data?.code === "FORBIDDEN";

  if (accessError) {
    return <div className="admin-frame"><div className="admin-callout"><strong>Admin access required.</strong> This area is restricted to the portfolio owner.</div></div>;
  }

  return <div className="admin-frame">
    <div className="admin-title-row">
      <div><p className="admin-kicker">TheKingOfAI / control surface</p><h1>Operate the<br />signal.</h1></div>
      <p>Private workspace for reviewing challenge briefs and publishing only evidence that is ready for public disclosure.</p>
    </div>

    <div className="admin-tabs" role="tablist" aria-label="Admin sections">
      <button className={`admin-tab ${tab === "overview" ? "active" : ""}`} onClick={() => setTab("overview")}><LayoutDashboard size={13} /> Overview</button>
      <button className={`admin-tab ${tab === "challenges" ? "active" : ""}`} onClick={() => setTab("challenges")}><Inbox size={13} /> Challenges</button>
      <button className={`admin-tab ${tab === "content" ? "active" : ""}`} onClick={() => setTab("content")}><FileText size={13} /> Content</button>
    </div>

    {tab === "overview" && <>
      <div className="admin-stat-grid">
        <Metric label="All challenge briefs" value={stats.data?.challenges} />
        <Metric label="New / unreviewed" value={stats.data?.newChallenges} />
        <Metric label="Article records" value={stats.data?.articles} />
        <Metric label="Case study records" value={stats.data?.caseStudies} />
      </div>
      <div className="admin-callout"><strong>Disclosure rule.</strong> Keep articles and case studies in Draft until the content is real, reviewed, and safe to publish. The public site intentionally hides empty sections.</div>
    </>}

    {tab === "challenges" && <section className="admin-panel">
      <div className="admin-panel-header"><h2>Challenge queue</h2><span>Newest first / max 100</span></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Brief</th><th>Contact</th><th>Received</th><th>Status</th></tr></thead><tbody>
        {(challenges.data ?? []).map((item) => <tr key={item.id}><td><strong>{item.challengeType}{item.challengeFocus ? ` / ${item.challengeFocus}` : ""}</strong><span>{item.brief.slice(0, 150)}{item.brief.length > 150 ? "…" : ""}</span></td><td><strong>{item.name}</strong><span>{item.email}</span></td><td>{dateLabel(item.createdAt)}</td><td><select className="admin-select" value={item.status} disabled={updateChallenge.isPending} onChange={(event) => updateChallenge.mutate({ id: item.id, status: event.target.value as "new" | "reviewing" | "replied" | "qualified" | "archived" })}><option value="new">New</option><option value="reviewing">Reviewing</option><option value="replied">Replied</option><option value="qualified">Qualified</option><option value="archived">Archived</option></select></td></tr>)}
      </tbody></table>{challenges.data?.length === 0 && <div className="admin-empty">No challenge briefs have arrived yet.</div>}</div>
    </section>}

    {tab === "content" && <>
      <div className="admin-content-grid">
        <section className="admin-panel"><div className="admin-panel-header"><h2><FileText size={16} /> New article</h2><span>Draft first</span></div><ArticleForm onCreated={refreshContent} /></section>
        <section className="admin-panel"><div className="admin-panel-header"><h2><BriefcaseBusiness size={16} /> New case study</h2><span>Approval required</span></div><CaseStudyForm onCreated={refreshContent} /></section>
      </div>
      <div className="admin-content-grid" style={{ marginTop: 16 }}>
        <section className="admin-panel"><div className="admin-panel-header"><h2>Article pipeline</h2><span>{articles.data?.length ?? 0} records</span></div><div className="admin-table-wrap"><table className="admin-table"><tbody>{(articles.data ?? []).map((article) => <tr key={article.id}><td><strong>{article.title}</strong><span>{article.slug}</span></td><td><span className="admin-status">{article.status}</span></td><td>{dateLabel(article.updatedAt)}</td></tr>)}</tbody></table>{articles.data?.length === 0 && <div className="admin-empty">No article records yet.</div>}</div></section>
        <section className="admin-panel"><div className="admin-panel-header"><h2>Outcome pipeline</h2><span>{caseStudies.data?.length ?? 0} records</span></div><div className="admin-table-wrap"><table className="admin-table"><tbody>{(caseStudies.data ?? []).map((study) => <tr key={study.id}><td><strong>{study.title}</strong><span>{study.slug}</span></td><td><span className="admin-status">{study.status}</span></td><td>{dateLabel(study.updatedAt)}</td></tr>)}</tbody></table>{caseStudies.data?.length === 0 && <div className="admin-empty">No case study records yet.</div>}</div></section>
      </div>
    </>}

    <a className="admin-public-link" href="/">Return to public site <ArrowUpRight size={14} /></a>
  </div>;
}

export default function Admin() {
  const { user } = useAuth();
  return <DashboardLayout><div className="admin-shell">{user ? <AdminContent /> : <div className="admin-frame"><div className="admin-callout">Sign in to access the private control surface.</div></div>}</div></DashboardLayout>;
}
