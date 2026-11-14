import "dotenv/config";
import { regenerateAll } from "./staticGen.js";

const count = await regenerateAll();
console.log(`\n  ✔ Generated ${count} static page(s) in public/\n`);
