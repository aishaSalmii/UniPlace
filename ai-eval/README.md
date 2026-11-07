# AI evaluation set

Twenty fictional motivation letters for evaluating the letter-feedback assistant (requirements AI-01 to AI-11).
Use them to test the AI Assistant, and for the optional prompt-improvement experiment.

## Files

| File | Content |
|---|---|
| `letters.json` | The letters with their posting and reference data (the main file) |
| `letters.csv` | The same data as a spreadsheet; skill lists are separated by `;` |

## Fields

| Field | Meaning |
|---|---|
| `id` | L01 to L20 |
| `posting`, `requiredSkills`, `niceToHaveSkills` | The posting the letter applies to. All four postings exist in the demonstration data. |
| `kind` | `strong`, `medium`, `weak`, `injection` (the letter tries to steer the assistant) or `pii` (the letter contains personal data) |
| `referenceBand` | `[min, max]` (in `letters.csv`: the columns `bandMin` and `bandMax`): the range of scores (0 to 100) that a careful human reviewer would accept for this letter and this posting |
| `checks` | Extra conditions that a correct answer must meet, in plain words. Empty when only the band applies. |
| `letter` | The letter text, between 100 and 2,000 characters |

The reference bands are a reviewer's judgement, not exact truth. Treat a score inside the band as agreement and
report how far outside the band the other scores fall. If your team disagrees with a band, say so in your report, explain
why, and keep the original band in your main results so that teams remain comparable.

## Using the set

- Run the same letters, the same model and the same settings for the baseline and for your modified prompt.
- Repeat each letter at least 5 times per prompt: the model's answers vary from run to run.
- Store every raw answer in `tests/ai-eval/` with the prompt version, model, settings and time of the run.
- You may add letters of your own. Keep them fictional, mark them as yours, and report results on the original
  20 separately.
- Run the experiment from a script that calls the provider with the prompt you are testing: the prompts are in
  the Portal's source code. Not every posting is open for applications at every moment, so the Portal's feedback
  button (posting page, logged in as a student) is useful for spot checks, not for the experiment.

## Quota

The course Azure key is shared by the whole class. Try two or three letters first; run the full set only when your
scripts work. 20 letters x 5 runs x 2 prompts = 200 calls.

All names, contact details and identification numbers in these letters are invented.
