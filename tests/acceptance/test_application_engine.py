"""Black-box contract tests. Start the Engine first; Python standard library only.
python tests/acceptance/test_application_engine.py [--start]
--start launches the supplied Engine in this process and stops it afterwards.
"""
import unittest,json,urllib.request,urllib.error,subprocess,time,sys
from pathlib import Path
from datetime import date,timedelta
ROOT=Path(__file__).resolve().parents[2]
BASE='http://127.0.0.1:8081/api/v1'
TRACE=[]
def call(path,body):
 req=urllib.request.Request(BASE+path,json.dumps(body).encode(),{'Content-Type':'application/json'})
 try:
  with urllib.request.urlopen(req,timeout=10) as r: result=(r.status,json.load(r))
 except urllib.error.HTTPError as e: result=(e.code,json.load(e))
 TRACE.append(dict(endpoint=path,request=body,status=result[0],response=result[1]));return result
def eligibility():
 d=date(2026,11,12)
 return dict(gpa=3,creditsCompleted=60,standing='GOOD',priorInternships=0,minGpa=2,minCredits=45,asOf=str(d),documents=[dict(type='CV',sizeBytes=100000,mimeType='application/pdf'),dict(type='TRANSCRIPT',sizeBytes=200000,mimeType='application/pdf',issueDate=str(d-timedelta(days=30)))])
def allowance(**kw):
 p=dict(monthlyAllowance=3000,startDate='2026-01-01',endDate='2026-01-30',weeklyHours=29,lateDays=0,transportSupplement=300,monthlyCap=5000,penaltyRate=.05,penaltyCap=.2);p.update(kw);return p
class Contract(unittest.TestCase):
 def test_E01_eligible_baseline(self):
  status,r=call('/eligibility',eligibility());self.assertEqual(status,200);self.assertTrue(r['eligible']);self.assertEqual(r['reasons'],[])
 def test_E02_gpa_equal_minimum(self):
  p=eligibility();p['gpa']=2;self.assertTrue(call('/eligibility',p)[1]['eligible'])
 def test_E03_transcript_day180(self):
  p=eligibility();p['documents'][1]['issueDate']=str(date.fromisoformat(p['asOf'])-timedelta(days=179));self.assertTrue(call('/eligibility',p)[1]['eligible'])
 def test_E04_transcript_day181(self):
  p=eligibility();p['documents'][1]['issueDate']=str(date.fromisoformat(p['asOf'])-timedelta(days=180));self.assertFalse(call('/eligibility',p)[1]['eligible'])
 def test_E05_all_reasons(self):
  p=eligibility();p.update(gpa=1,creditsCompleted=1,documents=[]);r=call('/eligibility',p)[1];self.assertFalse(r['eligible']);self.assertGreaterEqual(len(r['reasons']),4)
 def test_E06_invalid_gpa(self):
  p=eligibility();p['gpa']=4.01;s,r=call('/eligibility',p);self.assertEqual(s,400);self.assertEqual(r['field'],'gpa')
 def test_E07_skill_normalization(self):
  s,r=call('/match',dict(studentSkills=[' java '],requiredSkills=['JAVA'],niceToHaveSkills=['Unmatched']));self.assertEqual(s,200);self.assertEqual(r['score'],70)
 def test_E08_empty_nice_skills(self):
  self.assertEqual(call('/match',dict(studentSkills=['Java'],requiredSkills=['Java'],niceToHaveSkills=[]))[1]['score'],70)
 def test_E09_half_rounds_up(self):
  p=dict(studentSkills=['A'],requiredSkills=['A','B','C','D'],niceToHaveSkills=['Unmatched']);self.assertEqual(call('/match',p)[1]['score'],18)
 def test_E10_ranking_tiebreak(self):
  p=dict(seats=1,candidates=[dict(applicationId=2,score=80,gpa=3,submittedAt='2026-11-12T10:00:00+03:00'),dict(applicationId=1,score=80,gpa=3,submittedAt='2026-11-12T09:00:00+03:00')]);s,r=call('/rank',p);self.assertEqual(s,200);self.assertEqual(r['ranked'],[1,2]);self.assertEqual(r['aboveCut'],[1])
 def test_E11_rank_zero_seats(self):
  s,r=call('/rank',dict(seats=0,candidates=[]));self.assertEqual(s,400);self.assertEqual(r['field'],'seats')
if __name__=='__main__':
 start='--start' in sys.argv
 if start:sys.argv.remove('--start')
 proc=None
 try:
  if start:
   proc=subprocess.Popen(['java','-jar',str(ROOT/'engine/uniplace-engine.jar')],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
   for i in range(40):
    try:urllib.request.urlopen(BASE+'/health',timeout=1);break
    except (OSError,urllib.error.URLError):time.sleep(.25)
  suite=unittest.defaultTestLoader.loadTestsFromTestCase(Contract)
  (ROOT/'evidence').mkdir(exist_ok=True)
  with (ROOT/'evidence/application-engine-results.txt').open('w') as f:r=unittest.TextTestRunner(stream=f,verbosity=2).run(suite)
  (ROOT/'evidence/application-engine-trace.json').write_text(json.dumps(TRACE,indent=2));print((ROOT/'evidence/application-engine-results.txt').read_text());sys.exit(not r.wasSuccessful())
 finally:
  if proc:proc.terminate();proc.wait(timeout=10)
