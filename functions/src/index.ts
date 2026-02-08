import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";
import OpenAI from "openai";

if (!admin.apps.length) admin.initializeApp();
const db = admin.firestore();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");
const openai = new OpenAI({ apiKey: OPENAI_API_KEY.value() });

export const recommendTrip = onRequest(
  { secrets: [OPENAI_API_KEY], region: "asia-northeast3", timeoutSeconds: 120 },
  async (req, res) => {
    // CORS
    res.set("Access-Control-Allow-Origin", "*");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
    if (req.method === "OPTIONS") return res.status(204).send();
    if (req.method !== "POST") return res.status(405).json({ success: false, message: "Use POST" });

    try {
      // 1) 사용자 인증 (Firebase ID Token)
      const authHeader = req.headers.authorization || "";
      const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
      if (!token) return res.status(401).json({ success: false, message: "No ID token" });
      const decoded = await admin.auth().verifyIdToken(token);
      const uid = decoded.uid;

      // 2) 입력 검증
      const { country, startDate, endDate, gender, age, preference } = req.body ?? {};
      if (!country || !startDate || !endDate) {
        return res.status(400).json({ success: false, message: "country, startDate, endDate는 필수입니다." });
      }
      const start = new Date(startDate);
      const end = new Date(endDate);
      const diffDays = Math.max(0, Math.floor((end.getTime() - start.getTime()) / (1000*60*60*24)));
      const nights = diffDays; const tripDays = diffDays + 1;

      // 3) LLM 호출(스키마 강제)
      const prefStr = Array.isArray(preference) ? preference.join(", ") : (preference ?? "미입력");
      const response = await openai.responses.create({
        model: "gpt-4.1-mini",
        input: `
너는 여행 일정 플래너다. JSON만 생성하라.
- 여행지: ${country}
- 기간: ${startDate} ~ ${endDate} (${nights}박 ${tripDays}일)
- 성별: ${gender || "미입력"}
- 연령대: ${age ?? "미입력"}
- 취향: ${prefStr}
        `.trim(),
        temperature: 0.7,
        max_output_tokens: 900,
        response_format: {
          type: "json_schema",
          json_schema: {
            name: "TripPlan",
            schema: {
              type: "object",
              properties: {
                days: {
                  type: "array",
                  items: {
                    type: "object",
                    properties: {
                      day: { type: "integer" },
                      title: { type: "string" },
                      places: {
                        type: "array",
                        items: {
                          type: "object",
                          properties: {
                            name: { type: "string" },
                            description: { type: "string" }
                          },
                          required: ["name"]
                        }
                      }
                    },
                    required: ["day", "title", "places"]
                  }
                }
              },
              required: ["days"]
            }
          }
        }
      });

      const content = response.output?.[0]?.content?.[0];
      const parsed = (content?.type === "output_json" ? content.parsed : null) as { days: any[] } | null;
      if (!parsed || !Array.isArray(parsed.days)) {
        return res.status(200).json({ success: false, message: "LLM invalid JSON" });
      }

      // 4) Firestore 저장
      const planRef = db.collection("itineraries").doc(uid).collection("plans").doc();
      const planDoc = {
        country,
        startDate,
        endDate,
        nights,
        days: tripDays,
        gender: gender ?? null,
        age: age ?? null,
        preference: Array.isArray(preference) ? preference : [],
        model: "gpt-4.1-mini",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        status: "done",
        error: null,
        daysJson: parsed.days, // ☆ 작은 일정은 바로 문서에 저장
      };
      await planRef.set(planDoc);

      // (대용량이면 하위 컬렉션에 저장하는 방식으로 전환)
      // await planRef.set({... , daysJson: admin.firestore.FieldValue.delete()});
      // await Promise.all(parsed.days.map(d => planRef.collection("days").doc(String(d.day)).set(d)));

      // 5) 응답: planId만 돌려주면, 앱은 Firestore로 읽어감
      return res.status(200).json({
        success: true,
        planId: planRef.id,
      });
    } catch (e: any) {
      console.error(e);
      return res.status(500).json({ success: false, message: e?.message ?? "Server error" });
    }
  }
);
