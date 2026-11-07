import { ArrowLeft, Clock3 } from "lucide-react";
import { Streamdown } from "streamdown";
import { useRoute } from "wouter";
import { trpc } from "@/lib/trpc";
import "./thinking.css";

export default function Thinking() {
  const [, params] = useRoute("/thinking/:slug");
  const article = trpc.portfolio.articleBySlug.useQuery({ slug: params?.slug ?? "" }, { enabled: Boolean(params?.slug) });

  if (article.isLoading) return <main className="thinking-shell"><div className="thinking-frame"><p className="thinking-kicker">Loading article / signal</p><div className="thinking-skeleton" /></div></main>;
  if (!article.data) return <main className="thinking-shell"><div className="thinking-frame"><a className="thinking-back" href="/"> <ArrowLeft size={15} /> Return to the public site</a><h1>Article unavailable.</h1><p>This article is either still a draft or the link is no longer public.</p></div></main>;

  return <main className="thinking-shell">
    <div className="thinking-frame">
      <a className="thinking-back" href="/"><ArrowLeft size={15} /> Back to TheKingOfAI</a>
      <p className="thinking-kicker">Technical thinking / {article.data.slug}</p>
      <h1>{article.data.title}</h1>
      <div className="thinking-meta"><span><Clock3 size={14} /> {article.data.readingTime} min read</span><span>{article.data.publishedAt ? new Date(article.data.publishedAt).toLocaleDateString() : "Published"}</span></div>
      <p className="thinking-excerpt">{article.data.excerpt}</p>
      <article className="thinking-body"><Streamdown>{article.data.content}</Streamdown></article>
    </div>
  </main>;
}
