/**
 * Terminal Editorial: a challenge-first public v1 built around capability,
 * engineering judgment, and a lightweight two-step intake.
 */
import { FormEvent, useState } from "react";
import "./expansion.css";
import { trpc } from "@/lib/trpc";
import {
  ArrowDownRight,
  ArrowUpRight,
  CheckCheck,
  CircleHelp,
  Download,
  Github,
  Mail,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { toast } from "sonner";

const githubUrl = "https://github.com/TheKingOfAI";

const challengeGroups = [
  { label: "Build", options: ["Website", "Web application", "Mobile application", "Digital product", "AI system", "Custom software / system"] },
  { label: "Fix / improve", options: ["Existing digital product", "Website / application", "Automation / workflow", "Architecture / system", "AI system"] },
  { label: "Automate", options: ["Business workflow", "Repetitive process", "Data workflow", "AI-assisted workflow", "System integration"] },
  { label: "Analyze", options: ["Product", "Technical feasibility", "Financial", "Quantitative", "Risk", "Data / system"] },
  { label: "Solve", options: [] },
  { label: "Other", options: [] },
];

const showDeferredContent = false;

const products = [
  { id: "01", name: "Product clarity sprint", description: "A focused review of the product, constraints, and technical direction before a costly build begins.", price: 950 },
  { id: "02", name: "Production build", description: "A scoped web product, internal tool, or AI-enabled workflow built with a clear delivery plan and working handoff.", price: 4200 },
  { id: "03", name: "Systems rescue", description: "A practical intervention for a fragile application, automation, or integration that needs diagnosis and a safer next step.", price: 1800 },
];

const currencyRates = { USD: 1, EGP: 49.5, EUR: 0.92 } as const;
type Currency = keyof typeof currencyRates;

const arabicCopy = {
  nav: { challenge: "ابدأ من التحدّي", capabilities: "إيه اللي بقدّمه", principles: "بشتغل إزاي" },
  headerCta: "احكيلي عن التحدّي",
  mobileCta: "التحدّي",
  heroEyebrow: "تفكير منظومي لمشروعات المنتج الصعبة",
  heroTitle: "مطور Full-stack. <span>وببني حلول AI بوعي.</span>",
  heroDescription: "<strong>مفيش قالب ثابت. ومفيش تصنيف واحد.</strong> ابعت المشكلة زي ما هي. من تحليلها وتصميم المنتج، للهندسة والتنفيذ والاختبار والتحسين — بنبني الحل على المطلوب فعلًا.",
  challenge: "ابدأ التحدّي",
  explore: "شوف الإمكانيات",
  based: "الموقع", basedValue: "القاهرة / عن بُعد",
  mode: "طريقة الشغل", modeValue: "بين المنتج والأنظمة",
  availability: "التوفر", availabilityValue: "متاح للمشروعات الصعبة",
  operatingRange: "نطاق الشغل",
  range: ["نبني", "نحل", "نؤتمت", "نحسّن", "نحلّل", "هات التحدّي"],
  challengeEyebrow: "أول نقاش بيكون تقني",
  challengeTitle: "معظم البورتفوليوهات بتعرض اللي اتبنى قبل كده. <span>أنا مهتم أشوف المشكلة اللي لسه محدش حلّها.</span>",
  challengeIntro: "هات المشكلة الصعبة. هنفكّك القيود، نختبر قابلية التنفيذ، ونختار الطريق الأنسب قبل ما نصرف وقت وفلوس على اتجاه غلط.",
  challengeResponse: "نفهم المشكلة والقيود والجدوى — وبعدها نتحرك.",
  category: "نوع التحدّي", optional: "اختياري", focus: "التركيز",
  describe: "احكي المشكلة", describePlaceholder: "إيه اللي صعب؟ إيه الموجود دلوقتي؟ وإيه اللي معطّل التقدّم؟",
  contactDetails: "بيانات التواصل", name: "الاسم", email: "الإيميل", constraints: "القيود", timeline: "المدة المتوقعة", budget: "الميزانية", tools: "الأدوات الحالية", productUrl: "رابط المنتج الحالي", links: "روابط مفيدة", optionalPlaceholder: "اختياري",
  send: "ابعت التحدّي", continue: "كمّل البيانات", sending: "جاري الإرسال…",
  capabilitiesEyebrow: "مدى واسع، يقوده الحكم الهندسي",
  capabilitiesTitle: "القدرة أهم من اسم التصنيف.",
  capabilitiesAside: "التكنولوجيا وسيلة، مش غاية. السؤال الأهم: النظام لازم يحقق إيه؟ ولازم يفضل شغال تحت أي ضغط؟",
  lifecycle: ["المشكلة", "التحليل", "تصميم المنتج", "الهندسة", "التنفيذ", "الاختبار", "التحسين"],
  evidence: "أمثلة على نطاق الشغل / مواقع · تطبيقات · منتجات موبايل · APIs · أنظمة AI · أدوات بيانات · أنظمة سحابية",
  profile: "الملف التعريفي / السيرة الذاتية", pullProfile: "تحميل الملف",
  contactEyebrow: "جاهز لبريف واضح ومدروس",
  contactTitle: "نبدأ من القيد الحقيقي.",
  contactBody: "نموذج التحدّي بيجمع التفاصيل في مكان واحد. ولو تفضّل الكلام المباشر، الإيميل مفتوح.",
  emailPrompt: "تفضّل الإيميل؟", emailBody: "ابعت السياق اللي عندك ونبدأ منه.", top: "فوق",
  footer: "© 2026 TheKingOfAI",
};

const arabicChallengeGroups: Record<string, string> = {
  Build: "نبني",
  "Fix / improve": "نصلّح / نحسّن",
  Automate: "نؤتمت",
  Analyze: "نحلّل",
  Solve: "نحل",
  Other: "حاجة تانية",
};

const outcomes = [
  {
    id: "01",
    category: "AI + automation",
    title: "Approved outcome record",
    summary: "A client-approved before / after account can appear here without disclosing the workflow, implementation, or operating logic behind it.",
    status: "client approval required",
    image: "/thekingofai-hero.svg",
    detail: "Reserved outcome space",
  },
  {
    id: "02",
    category: "Product engineering",
    title: "Confidential delivery record",
    summary: "Product visuals, a concise delivery summary, and an observable result are shared only when the product owner has cleared them for public view.",
    status: "private implementation",
    image: "/thekingofai-hero.svg",
    detail: "Disclosure by permission",
  },
  {
    id: "03",
    category: "Systems + data",
    title: "Future public case study",
    summary: "This format is ready for a problem statement, delivered outcome, client role, and live link—without turning proprietary work into a reconstruction guide.",
    status: "evidence pending approval",
    image: "/thekingofai-hero.svg",
    detail: "Client-safe evidence",
  },
];

const capabilities = [
  { id: "01", title: "Web & application engineering", copy: "Websites, web applications, mobile products, SaaS, APIs, and production software that meet the actual operating need." },
  { id: "02", title: "AI systems & agents", copy: "Useful intelligence with evaluation, oversight, and failure paths designed in—not bolted on later." },
  { id: "03", title: "Automation", copy: "Manual processes, integrations, and repetitive work examined as systems, then made more reliable and observable." },
  { id: "04", title: "Digital products", copy: "Ideas, workflows, and rough concepts shaped into usable products—or diagnosed before unnecessary rebuilding begins." },
  { id: "05", title: "System architecture", copy: "Clear boundaries, maintainability, security, and technical decisions that fit the real constraints." },
  { id: "06", title: "Product engineering", copy: "Requirements and business problems turned into products that can be validated, measured, and improved." },
  { id: "07", title: "Financial & quantitative systems", copy: "Financial modeling, quantitative research, risk analysis, dashboards, decision support, and analytical tooling." },
  { id: "08", title: "Complex problem solving", copy: "Difficult technical and operational problems that do not fit neatly into a predefined service category." },
];

const principles = [
  ["01", "Architecture first", "The shape of a system is decided before its complexity is allowed to spread."],
  ["02", "Reliability is a feature", "Happy paths matter. So do exceptions, recovery, and what happens at the edges."],
  ["03", "Explainable choices", "Technical decisions should remain legible to the people who must own them next."],
  ["04", "Production thinking", "Security, testability, maintainability, and measurable outcomes belong in the first conversation."],
];

const arabicCapabilities = [
  ["01", "هندسة المواقع والتطبيقات", "مواقع، تطبيقات ويب وموبايل، SaaS، APIs، وبرمجيات جاهزة للاستخدام الفعلي."],
  ["02", "أنظمة AI والوكلاء", "ذكاء مفيد، مع تقييم واضح، وإشراف بشري، وخطط للتعامل مع الفشل من البداية."],
  ["03", "الأتمتة", "نحوّل العمليات اليدوية والتكاملات المتكررة إلى أنظمة أكثر ثباتًا وقابلية للمتابعة."],
  ["04", "المنتجات الرقمية", "نحوّل الفكرة أو الـ workflow غير المكتمل لمنتج قابل للاستخدام — أو نكتشف بدري إن إعادة البناء مش هي الحل."],
  ["05", "هندسة الأنظمة", "حدود واضحة، قابلية صيانة، أمان، وقرارات تقنية متوافقة مع القيود الحقيقية."],
  ["06", "هندسة المنتج", "نحوّل احتياج البزنس لمتطلبات ومنتج قابل للاختبار والقياس والتحسين."],
  ["07", "الأنظمة المالية والكمية", "نمذجة مالية، بحث كمي، تحليل مخاطر، dashboards، وأدوات تساعدك تاخد قرار أحسن."],
  ["08", "حل المشكلات المعقدة", "مشكلات تقنية وتشغيلية مش بتركب على خدمة جاهزة، وبتحتاج تفكير منظم قبل التنفيذ."],
];

const arabicPrinciples = [
  ["01", "الهندسة قبل الكود", "شكل النظام بيتحدد قبل ما التعقيد ينتشر."],
  ["02", "الاعتمادية جزء من المنتج", "المسار الطبيعي مهم، لكن الأهم كمان الاستثناءات، والاسترجاع، وكل اللي بيحصل على الأطراف."],
  ["03", "قرارات مفهومة", "القرار التقني لازم يفضل واضح للناس اللي هتملكه وتطوره بعد كده."],
  ["04", "تفكير الإنتاج من أول يوم", "الأمان، وقابلية الاختبار والصيانة، والنتائج القابلة للقياس جزء من أول نقاش."],
];

const writingTopics = [
  ["Draft queue / 01", "Reliable AI systems are designed around uncertainty."],
  ["Draft queue / 02", "The failure modes that matter in automation."],
  ["Draft queue / 03", "Architecture trade-offs are product decisions."],
];

function Brand() {
  return (
    <a className="brand-lockup" href="#top" aria-label="TheKingOfAI home">
      <span className="brand-symbol"><img className="brand-mark" src="/thekingofai-logo.svg" alt="" /></span>
      <span className="brand-wordmark">THEKINGOFAI</span>
    </a>
  );
}

export default function Home() {
  const [isArabic, setIsArabic] = useState(false);
  const [currency, setCurrency] = useState<Currency>("USD");
  const language = arabicCopy;
  const [challengeGroup, setChallengeGroup] = useState<string | null>(null);
  const [challengeOption, setChallengeOption] = useState<string | null>(null);
  const [intakeStep, setIntakeStep] = useState(false);
  const activeChallengeGroup = challengeGroups.find((group) => group.label === challengeGroup);
  const challengeMutation = trpc.challenge.create.useMutation();
  const { data: publishedContent } = trpc.portfolio.publishedContent.useQuery();

  const handleChallenge = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const category = [challengeGroup, challengeOption].filter(Boolean).join(" / ") || "Open problem";
    const brief = String(form.get("challenge-brief") || "");

    if (!intakeStep) {
      setIntakeStep(true);
       toast.message(isArabic ? "أضف بيانات التواصل لما تكون جاهز تبعت البريف." : "Add contact details when you’re ready to send the brief.");
      return;
    }

    const name = String(form.get("name") || "");
    const email = String(form.get("email") || "");
    const payload = {
      name,
      email,
      challengeType: challengeGroup || "Open problem",
      challengeFocus: challengeOption || undefined,
      brief,
      constraints: String(form.get("constraints") || "") || undefined,
      timeline: String(form.get("timeline") || "") || undefined,
      budget: String(form.get("budget") || "") || undefined,
      tools: String(form.get("tools") || "") || undefined,
      productUrl: String(form.get("product-url") || "") || undefined,
      links: String(form.get("links") || "") || undefined,
    };

    try {
      await challengeMutation.mutateAsync(payload);
       toast.success(isArabic ? "وصل التحدّي. هراجعه وأرجعلك قبل ما نبدأ." : "Signal received. I’ll review the problem before responding.");
      event.currentTarget.reset();
      setChallengeGroup(null);
      setChallengeOption(null);
      setIntakeStep(false);
    } catch {
      const subject = encodeURIComponent(`Challenge brief — ${category}`);
      const body = encodeURIComponent(`Name: ${name}\nEmail: ${email}\nChallenge type: ${category}\n\nProblem:\n${brief}\n\nThe database endpoint was unavailable. Please review this brief manually.`);
       toast.error(isArabic ? "تعذّر الإرسال من النموذج. هفتح لك الإيميل كبديل." : "The secure endpoint was unavailable. Opening an email fallback.");
      window.setTimeout(() => { window.location.href = `mailto:hello@thekingofai.dev?subject=${subject}&body=${body}`; }, 220);
    }
  };

  return (
    <div className="site-shell" dir={isArabic ? "rtl" : "ltr"}>
      <header className="site-header">
        <div className="header-inner">
          <Brand />
          <nav className="desktop-nav" aria-label={isArabic ? "التنقل الرئيسي" : "Primary navigation"}>
            <a href="#challenge">{isArabic ? language.nav.challenge : "Challenge"}</a>
            <a href="#capabilities">{isArabic ? language.nav.capabilities : "Capabilities"}</a>
            <a href="#products">{isArabic ? "المنتجات" : "Products"}</a>
            <a href="#principles">{isArabic ? language.nav.principles : "How I work"}</a>
          </nav>
          <button type="button" className="language-toggle" onClick={() => setIsArabic((value) => !value)} aria-label={isArabic ? "Switch to English" : "التبديل للعربية"}>{isArabic ? "EN" : "عربي"}</button>
          <a href="#challenge" className="header-contact">{isArabic ? language.headerCta : "Start a challenge"} <ArrowUpRight size={15} aria-hidden="true" /></a>
          <a className="mobile-menu" href="#challenge">{isArabic ? language.mobileCta : "Challenge"} <ArrowDownRight size={15} aria-hidden="true" /></a>
        </div>
      </header>

      <main id="top">
        <section className="hero section-frame" aria-labelledby="hero-title">
          <div className="vertical-index hero-index">00 / operating statement</div>
          <div className="hero-copy">
            <p className="eyebrow"><span className="status-dot" /> {isArabic ? language.heroEyebrow : "Systems thinking for difficult product work"}</p>
            <h1 id="hero-title">{isArabic ? <>مطور Full-stack.<span>وببني حلول AI بوعي.</span></> : <>Full-stack developer.<span>AI-minded builder.</span></>}</h1>
            {isArabic ? <p className="hero-description" dangerouslySetInnerHTML={{ __html: language.heroDescription }} /> : <p className="hero-description"><strong>No fixed stack. No fixed category.</strong> Bring the problem. I work from analysis through product design, architecture, build, validation, and improvement.</p>}
            <div className="hero-actions">
              <a href="#challenge" className="signal-button">{isArabic ? language.challenge : "Challenge me"} <ArrowDownRight size={19} aria-hidden="true" /></a>
              <a href="#capabilities" className="ghost-button">{isArabic ? language.explore : "Explore capabilities"} <ArrowDownRight size={18} aria-hidden="true" /></a>
            </div>
          </div>
          <div className="hero-visual" aria-hidden="true">
             <img src="/thekingofai-hero.svg" alt="" />
            <div className="visual-caption"><span>Constraint study / 001</span><span>Examine first</span></div>
          </div>
          <div className="hero-meta">
             <div><span className="meta-label">{isArabic ? language.based : "Based"}</span><strong>{isArabic ? language.basedValue : "Cairo / Remote"}</strong></div>
             <div><span className="meta-label">{isArabic ? language.mode : "Working mode"}</span><strong>{isArabic ? language.modeValue : "Across product + systems"}</strong></div>
             <div><span className="meta-label">{isArabic ? language.availability : "Availability"}</span><strong className="live-label">{isArabic ? language.availabilityValue : "Open to hard briefs"}</strong></div>
          </div>
        </section>

         <section className="stack-strip" aria-label="Problem-solving range">
           <div className="stack-label">{isArabic ? language.operatingRange : "Operating range"}</div>
           <div className="stack-items">{(isArabic ? language.range : ["Build", "Solve", "Automate", "Improve", "Analyze", "Challenge me"]).map((item) => <span key={item}>{item}</span>)}</div>
         </section>

         <section id="products" className="products-section section-frame" aria-labelledby="products-title">
           <div className="section-heading-row">
             <div className="vertical-index">02 / products</div>
             <div className="heading-block"><p className="eyebrow">{isArabic ? "طرق واضحة للبدء" : "Clear ways to start"}</p><h2 id="products-title">{isArabic ? "اختار نقطة البداية المناسبة." : "Choose the right starting point."}</h2></div>
             <div className="currency-control"><label htmlFor="currency">{isArabic ? "العملة" : "Currency"}</label><select id="currency" value={currency} onChange={(event) => setCurrency(event.target.value as Currency)}><option value="USD">USD</option><option value="EGP">EGP</option><option value="EUR">EUR</option></select></div>
           </div>
           <div className="product-grid">
             {products.map((product) => <article className="product-card" key={product.id}>
               <span className="product-id">{product.id}</span>
               <h3>{isArabic ? ({ "Product clarity sprint": "جلسة وضوح المنتج", "Production build": "بناء منتج جاهز", "Systems rescue": "إنقاذ نظام" } as Record<string, string>)[product.name] : product.name}</h3>
               <p>{isArabic ? ({ "Product clarity sprint": "مراجعة مركزة للمنتج والقيود والاتجاه التقني قبل بدء بناء مكلف.", "Production build": "منتج ويب أو أداة داخلية أو workflow مدعوم بالـ AI مع خطة تسليم واضحة.", "Systems rescue": "تدخل عملي لنظام أو أتمتة أو تكامل هش يحتاج تشخيص وخطوة تالية أكثر أمانًا." } as Record<string, string>)[product.name] : product.description}</p>
               <strong>{currency} {new Intl.NumberFormat(isArabic ? "ar-EG" : "en-US", { maximumFractionDigits: currency === "EGP" ? 0 : 2 }).format(product.price * currencyRates[currency])}</strong>
               <a href="#challenge" className="product-link">{isArabic ? "ابدأ من هنا" : "Start here"} <ArrowUpRight size={15} aria-hidden="true" /></a>
             </article>)}
           </div>
           <p className="pricing-note">{isArabic ? "الأسعار دي نقطة بداية للنطاق المذكور، وليست عرضًا نهائيًا. كل مشروع بيتسعّر بعد فهم القيود والنتيجة المطلوبة." : "These are starting prices for the stated scope, not a final quote. Every project is priced after the constraints and desired outcome are understood."}</p>
         </section>

         <section id="challenge" className="challenge-section section-frame" aria-labelledby="challenge-title">
          <div className="vertical-index">01 / challenge desk</div>
          <div className="challenge-copy">
             <p className="eyebrow"><Sparkles size={13} aria-hidden="true" /> {isArabic ? language.challengeEyebrow : "The first conversation is technical"}</p>
             {isArabic ? <h2 id="challenge-title" dangerouslySetInnerHTML={{ __html: language.challengeTitle }} /> : <h2 id="challenge-title">Most portfolios show what has already been built.<br /><span>I'd rather see what you think can't be built.</span></h2>}
             <p>{isArabic ? language.challengeIntro : "Bring the difficult problem. I’ll examine the constraints, feasibility, and the best way to approach it."}</p>
             <p className="challenge-response">{isArabic ? language.challengeResponse : "Let’s examine the problem, constraints, and economics."}</p>
          </div>
          <form className="challenge-form" onSubmit={handleChallenge}>
            <fieldset>
               <legend>{isArabic ? language.category : "Problem category"} <em>{isArabic ? language.optional : "optional"}</em></legend>
              <div className="challenge-group-options">
                 {challengeGroups.map((group) => <button type="button" key={group.label} aria-pressed={challengeGroup === group.label} className={challengeGroup === group.label ? "selected" : ""} onClick={() => { setChallengeGroup(group.label); setChallengeOption(null); }}>{isArabic ? arabicChallengeGroups[group.label] : group.label}</button>)}
              </div>
            </fieldset>
             {activeChallengeGroup && activeChallengeGroup.options.length > 0 && <fieldset className="challenge-subtype"><legend>{isArabic ? language.focus : `${activeChallengeGroup.label} focus`} <em>{isArabic ? language.optional : "optional"}</em></legend><div className="challenge-suboptions">{activeChallengeGroup.options.map((option) => <button type="button" key={option} aria-pressed={challengeOption === option} className={challengeOption === option ? "selected" : ""} onClick={() => setChallengeOption(option)}>{option}</button>)}</div></fieldset>}
             <label className="challenge-input-label"><span>{isArabic ? language.describe : "Describe the problem"}</span><textarea required name="challenge-brief" rows={4} placeholder={isArabic ? language.describePlaceholder : "What is difficult, what exists today, and what is getting in the way?"} /></label>
             {intakeStep && <div className="intake-details"><p>{isArabic ? language.contactDetails : "Contact details"}</p><div className="intake-contact-fields"><label><span>{isArabic ? language.name : "Name"}</span><input name="name" autoComplete="name" required placeholder={isArabic ? "اسمك" : "Your name"} /></label><label><span>{isArabic ? language.email : "Email"}</span><input name="email" type="email" autoComplete="email" required placeholder="you@company.com" /></label></div><div className="intake-context-fields"><label><span>{isArabic ? language.constraints : "Constraints"}</span><input name="constraints" placeholder={isArabic ? language.optionalPlaceholder : "Optional"} /></label><label><span>{isArabic ? language.timeline : "Timeline"}</span><input name="timeline" placeholder={isArabic ? language.optionalPlaceholder : "Optional"} /></label><label><span>{isArabic ? language.budget : "Budget"}</span><input name="budget" placeholder={isArabic ? language.optionalPlaceholder : "Optional"} /></label><label><span>{isArabic ? language.tools : "Existing tools"}</span><input name="tools" placeholder={isArabic ? language.optionalPlaceholder : "Optional"} /></label><label><span>{isArabic ? language.productUrl : "Existing product URL"}</span><input name="product-url" type="url" placeholder={isArabic ? language.optionalPlaceholder : "Optional"} /></label><label><span>{isArabic ? language.links : "Relevant links"}</span><input name="links" placeholder={isArabic ? "روابط اختيارية" : "Optional URLs"} /></label></div></div>}
             <button type="submit" className="signal-button" disabled={challengeMutation.isPending}>{challengeMutation.isPending ? (isArabic ? language.sending : "Sending…") : intakeStep ? (isArabic ? language.send : "Challenge me") : (isArabic ? language.continue : "Continue")} <ArrowUpRight size={18} aria-hidden="true" /></button>
          </form>
        </section>

        <section id="capabilities" className="capabilities-section section-frame" aria-labelledby="capabilities-title">
          <div className="section-heading-row">
            <div className="vertical-index">02 / capabilities</div>
             <div className="heading-block"><p className="eyebrow">{isArabic ? language.capabilitiesEyebrow : "Range, guided by judgment"}</p><h2 id="capabilities-title">{isArabic ? language.capabilitiesTitle : "Capability before category."}</h2></div>
             <p className="section-aside">{isArabic ? language.capabilitiesAside : "Technology is supporting evidence. The important question is what the system has to accomplish—and what it must withstand."}</p>
          </div>
          <div className="capability-grid">
             {(isArabic ? arabicCapabilities : capabilities.map((item) => [item.id, item.title, item.copy] as const)).map(([id, title, copy]) => (
               <article className="capability-card" key={id}>
                 <span className="capability-id">{id}</span>
                 <h3>{title}</h3>
                 <p>{copy}</p>
                <span className="capability-rule" />
              </article>
            ))}
          </div>
           <div className="lifecycle-strip">{(isArabic ? language.lifecycle : ["Problem", "Analysis", "Product design", "Architecture", "Build", "Validate", "Improve"]).map((item, index, items) => <><span key={item}>{item}</span>{index < items.length - 1 && <ArrowDownRight key={`${item}-arrow`} size={15} aria-hidden="true" />}</>)}</div>
           <div className="supporting-evidence"><span>{isArabic ? language.evidence : "Supporting evidence / websites · applications · mobile products · APIs · AI systems · data tools · cloud systems"}</span><a href="/thekingofai-profile.txt" download className="profile-link"><span><small>{isArabic ? language.profile : "Profile / Resume"}</small>{isArabic ? language.pullProfile : "Pull profile.txt"}</span><Download size={15} aria-hidden="true" /></a></div>
        </section>

        {publishedContent && (publishedContent.caseStudies.length > 0 || publishedContent.articles.length > 0) && <section className="live-content-section section-frame" aria-labelledby="live-content-title">
          <div className="section-heading-row"><div className="vertical-index">03 / published signal</div><div className="heading-block"><p className="eyebrow">Evidence, when it is ready</p><h2 id="live-content-title">Selected work and thinking.</h2></div><p className="section-aside">Only approved public records appear here. Drafts and private implementation notes stay inside the control surface.</p></div>
          {publishedContent.caseStudies.length > 0 && <div className="live-content-grid">{publishedContent.caseStudies.map((study) => <article className="live-content-card" key={study.id}><span className="live-content-type">Case study / {study.slug}</span><h3>{study.title}</h3><p>{study.summary}</p><span className="live-content-result">{study.outcome || "Outcome details available on request."}</span></article>)}</div>}
          {publishedContent.articles.length > 0 && <div className="live-article-list">{publishedContent.articles.map((article) => <article className="live-article-row" key={article.id}><div><span>{article.readingTime} min read / {article.slug}</span><h3>{article.title}</h3><p>{article.excerpt}</p></div><a href={`/thinking/${article.slug}`}>Read <ArrowUpRight size={15} aria-hidden="true" /></a></article>)}</div>}
        </section>}

        {showDeferredContent && <>
        <section id="outcomes" className="outcomes-section section-frame" aria-labelledby="outcomes-title">
          <div className="section-heading-row">
            <div className="vertical-index">03 / selected outcomes</div>
            <div className="heading-block"><p className="eyebrow">Capability, observed responsibly</p><h2 id="outcomes-title">Show the engineering capability.<br />Not the recipe.</h2></div>
            <p className="section-aside">Commercial implementation details stay private by default. Outcome proof is shared where client approval permits.</p>
          </div>
          <div className="privacy-note"><ShieldCheck size={18} aria-hidden="true" /><p><strong>Disclosure standard.</strong> A detail belongs here only when it proves capability without materially helping someone reproduce the underlying system.</p></div>
          <div className="outcome-grid">
            {outcomes.map((outcome) => (
              <article className={`outcome-card outcome-${outcome.id}`} key={outcome.id}>
                <div className="outcome-image-wrap"><img src={outcome.image} alt="Abstract technical system study" /><span className="outcome-number">Engagement {outcome.id}</span><span className="project-corner" /><div className="study-mark"><span>{outcome.category}</span><span>{outcome.status}</span></div></div>
                <div className="outcome-content"><p className="project-kicker">{outcome.detail}</p><h3>{outcome.title}</h3><p>{outcome.summary}</p><div className="outcome-status"><span><CheckCheck size={14} aria-hidden="true" /> {outcome.status}</span><a href="#contact">Discuss a comparable brief <ArrowUpRight size={14} aria-hidden="true" /></a></div></div>
              </article>
            ))}
          </div>
        </section>

        <section className="evidence-section section-frame" aria-labelledby="evidence-title">
          <div className="vertical-index">03.1 / trust layer</div>
          <div className="evidence-main"><p className="eyebrow">Client proof, with permission</p><h2 id="evidence-title">Trust should connect to the work it refers to.</h2><p>Testimonials, before/after descriptions, final screenshots, short product videos, and public product links will be attached to the relevant outcome record only after approval. Nothing here is invented to fill a grid.</p></div>
          <div className="evidence-cards">
            <div><span>01 / client feedback</span><strong>Published only with attribution approval.</strong><em>No testimonials published yet.</em></div>
            <div><span>02 / observable result</span><strong>Before / after evidence belongs beside the related work.</strong><em>Awaiting client-safe material.</em></div>
            <div><span>03 / public launch</span><strong>Live products are linked only when product owners choose disclosure.</strong><em>Links added on permission.</em></div>
          </div>
        </section>

        <section id="principles" className="principles-section section-frame" aria-labelledby="principles-title">
          <div className="section-heading-row"><div className="vertical-index">04 / engineering principles</div><div className="heading-block"><p className="eyebrow">How I work</p><h2 id="principles-title">Fast is useful. Durable is the point.</h2></div><p className="section-aside">Good engineering is more than rapid output. It is a set of choices that remain sound after the launch moment.</p></div>
          <div className="principle-list">
            {principles.map(([id, title, copy]) => <article key={id}><span>{id}</span><h3>{title}</h3><p>{copy}</p><CircleHelp size={17} aria-hidden="true" /></article>)}
          </div>
          <div className="premise-note"><span>Product principle / 05</span><p><strong>Not every idea deserves to be built.</strong> If the premise is commercially weak, technically fragile, or aimed at the wrong problem, that should be clear before time is spent implementing it.</p></div>
        </section>

        <section id="thinking" className="writing-section section-frame" aria-labelledby="thinking-title">
          <div className="section-heading-row writing-heading"><div className="vertical-index">05 / technical thinking</div><div className="heading-block"><p className="eyebrow">Writing in preparation</p><h2 id="thinking-title">Technical judgment, made visible.</h2></div><p className="section-aside">A publishing queue for decisions, trade-offs, and patterns that deserve a more careful explanation.</p></div>
          <div className="article-list">
            {writingTopics.map(([label, title], index) => <div className="article-row article-draft" key={title}><span className="article-index">0{index + 1}</span><div className="article-main"><div className="article-meta"><span>{label}</span><span>Unpublished</span></div><h3>{title}</h3></div><span className="article-read">In development</span><span className="article-arrow">—</span></div>)}
          </div>
          <div className="github-note"><Github size={18} aria-hidden="true" /><p><strong>Public GitHub is intentional.</strong> It is reserved for reusable utilities, small engineering demonstrations, experiments, and open-source contributions—not private commercial systems.</p><a href={githubUrl} target="_blank" rel="noreferrer">View public work <ArrowUpRight size={16} aria-hidden="true" /></a></div>
        </section>
        </>}

        <section id="contact" className="contact-section section-frame" aria-labelledby="contact-title">
          <div className="contact-copy"><div className="vertical-index">04 / {isArabic ? "تواصل" : "contact"}</div><p className="eyebrow"><span className="status-dot" /> {isArabic ? language.contactEyebrow : "Ready for a considered brief"}</p><h2 id="contact-title">{isArabic ? language.contactTitle : "Start with the real constraint."}</h2><p>{isArabic ? language.contactBody : "The challenge flow keeps the brief in one place. Prefer a direct note? Email works too."}</p><a href="#challenge" className="signal-button">{isArabic ? language.challenge : "Challenge me"} <ArrowUpRight size={17} aria-hidden="true" /></a></div>
          <div className="contact-panel"><span>{isArabic ? language.emailPrompt : "Prefer email?"}</span><p>{isArabic ? language.emailBody : "For a direct conversation, send the context you already have."}</p><a href="mailto:hello@thekingofai.dev" className="contact-email"><Mail size={18} aria-hidden="true" /> hello@thekingofai.dev</a></div>
        </section>
      </main>

      <footer className="site-footer"><div className="footer-brand"><span className="footer-symbol"><img src="/thekingofai-logo.svg" alt="" /></span><span>THEKINGOFAI</span></div><p>{isArabic ? language.footer : "© 2026 TheKingOfAI"}</p><a href="#top">{isArabic ? language.top : "Top"} <ArrowUpRight size={15} aria-hidden="true" /></a></footer>
    </div>
  );
}
