import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import api from '../../services/api';

const OfferStatus = () => {
  const [searchParams] = useSearchParams();
  const [emailInput, setEmailInput] = useState('');
  const [candidate, setCandidate] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [confettiActive, setConfettiActive] = useState(false);

  const emailParam = searchParams.get('email');

  useEffect(() => {
    if (emailParam) {
      fetchOfferStatus(emailParam);
    }
  }, [emailParam]);

  const fetchOfferStatus = async (email) => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get(`/candidate/offer-status?email=${encodeURIComponent(email)}`);
      setCandidate(response.data);
      if (response.data.recruitmentStage === 'HIRED') {
        setConfettiActive(true);
        setTimeout(triggerConfetti, 100);
      }
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Failed to fetch status. Please verify the email address.');
      setCandidate(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    if (emailInput.trim()) {
      fetchOfferStatus(emailInput.trim());
    }
  };

  const handleAcceptOffer = async () => {
    if (!candidate) return;
    setLoading(true);
    setError('');
    try {
      await api.put(`/candidate/offer-status/accept?email=${encodeURIComponent(candidate.email)}`);
      setCandidate(prev => ({ ...prev, recruitmentStage: 'HIRED' }));
      setConfettiActive(true);
      setTimeout(triggerConfetti, 100);
    } catch (err) {
      console.error(err);
      setError('Failed to accept the offer. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const triggerConfetti = () => {
    const canvas = document.getElementById('confetti-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    let particles = [];
    const colors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444', '#3B82F6', '#EC4899'];
    for (let i = 0; i < 150; i++) {
      particles.push({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height - canvas.height,
        r: Math.random() * 6 + 4,
        d: Math.random() * canvas.height,
        color: colors[Math.floor(Math.random() * colors.length)],
        tilt: Math.random() * 10 - 5,
        tiltAngleIncremental: Math.random() * 0.07 + 0.02,
        tiltAngle: 0
      });
    }
    function draw() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      particles.forEach((p, idx) => {
        p.tiltAngle += p.tiltAngleIncremental;
        p.y += (Math.cos(p.d) + 3 + p.r / 2) / 2;
        p.x += Math.sin(p.tiltAngle);
        p.tilt = Math.sin(p.tiltAngle - idx / 3) * 15;

        ctx.beginPath();
        ctx.lineWidth = p.r;
        ctx.strokeStyle = p.color;
        ctx.moveTo(p.x + p.tilt + p.r / 2, p.y);
        ctx.lineTo(p.x + p.tilt, p.y + p.tilt + p.r / 2);
        ctx.stroke();
      });
      if (particles.some(p => p.y < canvas.height)) {
        requestAnimationFrame(draw);
      }
    }
    draw();
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      try {
        const date = new Date(dateStr);
        if (!isNaN(date.getTime())) {
          return date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
        }
      } catch (e) {}
    }
    return dateStr;
  };

  // Define recruitment pipeline steps
  const steps = [
    { label: 'Applied', key: 'APPLICATION_SUBMITTED' },
    { label: 'Shortlisted', key: 'SHORTLISTED' },
    { label: 'Approved', key: 'HR_APPROVED' },
    { label: 'BGV In Progress', key: 'BGV_INITIATED' },
    { label: 'BGV Cleared', key: 'BGV_CLEARED' },
    { label: 'Offer Sent', key: 'OFFER_SENT' },
    { label: 'Hired', key: 'HIRED' },
  ];

  const getActiveStepIndex = () => {
    if (!candidate) return 0;
    const stage = candidate.recruitmentStage;
    if (stage === 'RESUME_SCREENING') return 0;
    const idx = steps.findIndex(s => s.key === stage);
    return idx !== -1 ? idx : 0;
  };

  const activeIndex = getActiveStepIndex();

  return (
    <div className="relative min-h-screen bg-slate-50 flex flex-col items-center py-12 px-4 sm:px-6 lg:px-8 font-sans">
      <canvas id="confetti-canvas" className="pointer-events-none fixed inset-0 z-50 w-full h-full"></canvas>

      {/* Header Area */}
      <div className="text-center mb-8 max-w-lg">
        <h1 className="text-4xl font-extrabold tracking-tight text-indigo-600 sm:text-5xl">
          Resume<span className="text-slate-900">Parser</span>
        </h1>
        <p className="mt-2 text-base text-slate-500 font-medium">Candidate Recruitment Offer Portal</p>
      </div>

      {/* Email Input Form (if candidate not loaded) */}
      {!candidate && !loading && (
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-slate-100 p-8">
          <h2 className="text-xl font-bold text-slate-800 mb-6 text-center">Track Your Application</h2>
          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-50 text-red-600 text-sm font-medium border border-red-100">
              {error}
            </div>
          )}
          <form onSubmit={handleSearchSubmit} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-semibold text-slate-700 mb-2">
                Enter your email address
              </label>
              <input
                id="email"
                type="email"
                required
                className="w-full px-4 py-3 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-slate-800 transition duration-150"
                placeholder="e.g. candidate@example.com"
                value={emailInput}
                onChange={(e) => setEmailInput(e.target.value)}
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl shadow-lg shadow-indigo-100 transition duration-150 transform active:scale-95"
            >
              Track Application Status
            </button>
          </form>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-2xl shadow-xl max-w-md w-full border border-slate-100">
          <div className="w-12 h-12 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
          <p className="mt-4 text-slate-600 font-medium">Loading details, please wait...</p>
        </div>
      )}

      {/* Main Candidate Card */}
      {candidate && !loading && (
        <div className="w-full max-w-3xl bg-white rounded-2xl shadow-xl border border-slate-100 overflow-hidden">
          {/* Confetti Banner */}
          {candidate.recruitmentStage === 'HIRED' && (
            <div className="bg-gradient-to-r from-emerald-500 to-teal-500 py-3 text-center text-white text-sm font-semibold tracking-wide uppercase animate-pulse">
              🎉 Congratulations! You have accepted our offer. Welcome aboard!
            </div>
          )}

          <div className="p-8">
            {/* Candidate Overview */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-100 pb-6 mb-8 gap-4">
              <div>
                <h2 className="text-2xl font-bold text-slate-800 capitalize">{candidate.fullName}</h2>
                <p className="text-slate-500 text-sm font-medium mt-1">{candidate.email} • {candidate.phoneNumber}</p>
              </div>
              <div className="flex items-center">
                <span className={`px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-wider ${
                  candidate.recruitmentStage === 'HIRED'
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-100'
                    : candidate.recruitmentStage === 'OFFER_SENT'
                    ? 'bg-indigo-50 text-indigo-700 border border-indigo-100'
                    : 'bg-amber-50 text-amber-700 border border-amber-100'
                }`}>
                  Stage: {candidate.recruitmentStage.replace('_', ' ')}
                </span>
              </div>
            </div>

            {/* Stepper */}
            <div className="mb-10">
              <h3 className="text-sm font-bold text-slate-400 uppercase tracking-wider mb-6">Selection Progress</h3>
              <div className="relative flex flex-col md:flex-row items-start md:items-center justify-between gap-6 md:gap-2">
                {steps.map((step, idx) => {
                  const isCompleted = idx < activeIndex;
                  const isActive = idx === activeIndex;
                  const isBGVInit = step.key === 'BGV_INITIATED' && candidate.recruitmentStage === 'BGV_INITIATED';

                  return (
                    <div key={step.key} className="flex md:flex-col items-center flex-1 w-full relative">
                      {/* Line connecting steps */}
                      {idx < steps.length - 1 && (
                        <div className="hidden md:block absolute left-1/2 right-[-50%] top-4 h-0.5 bg-slate-200 z-0">
                          <div 
                            className="h-full bg-indigo-600 transition-all duration-500" 
                            style={{ width: idx < activeIndex ? '100%' : '0%' }}
                          />
                        </div>
                      )}

                      <div className="flex items-center gap-4 md:flex-col md:gap-2 z-10">
                        {/* Circle Indicator */}
                        <div className={`w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm transition-all duration-300 ${
                          isCompleted
                            ? 'bg-indigo-600 text-white shadow-md shadow-indigo-100'
                            : isActive
                            ? isBGVInit
                              ? 'bg-amber-500 text-white shadow-md animate-pulse'
                              : 'bg-indigo-600 text-white ring-4 ring-indigo-100'
                            : 'bg-slate-100 text-slate-400 border border-slate-200'
                        }`}>
                          {isCompleted ? (
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7" />
                            </svg>
                          ) : (
                            idx + 1
                          )}
                        </div>

                        {/* Label */}
                        <div className="text-left md:text-center">
                          <p className={`text-sm font-bold ${
                            isActive ? 'text-indigo-600' : isCompleted ? 'text-slate-800' : 'text-slate-400'
                          }`}>
                            {step.label}
                          </p>
                          {isActive && isBGVInit && (
                            <span className="text-[10px] text-amber-500 font-semibold uppercase animate-pulse">Verification Active</span>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Stage Specific Instructions */}
            {candidate.recruitmentStage === 'BGV_INITIATED' && (
              <div className="bg-amber-50 border border-amber-100 rounded-xl p-5 mb-8">
                <h4 className="text-amber-800 font-bold text-sm mb-1">Background Verification In Progress</h4>
                <p className="text-amber-700 text-xs leading-relaxed">
                  Our verification team is validating your qualifications, identity, and background details. No action is required from you. We will update you once clearance is received.
                </p>
              </div>
            )}

            {/* Offer Letter Display Area */}
            {(candidate.recruitmentStage === 'OFFER_SENT' || candidate.recruitmentStage === 'HIRED') && (
              <div className="border border-slate-150 rounded-2xl bg-slate-50/50 p-6 sm:p-8 mb-8">
                <div className="bg-white border border-slate-100 rounded-xl shadow-sm p-6 sm:p-8 font-serif leading-relaxed text-slate-800 max-w-2xl mx-auto">
                  {/* Internal Offer Letter Styling */}
                  <div className="text-center mb-8 font-sans">
                    <h3 className="text-xl font-extrabold text-slate-900 tracking-wide">LETTER OF EMPLOYMENT</h3>
                    <p className="text-slate-400 text-xs uppercase tracking-wider mt-1">Confidential</p>
                  </div>

                  <p className="text-sm text-slate-600 mb-6">Date: {new Date().toLocaleDateString()}</p>
                  
                  <p className="mb-4">Dear <strong className="capitalize text-slate-900">{candidate.fullName}</strong>,</p>
                  
                  <p className="mb-4">
                    Following your interviews and successful Background Verification clearances, we are delighted to offer you employment with <strong>ResumeParser Corporation</strong>.
                  </p>

                  <h4 className="font-sans font-bold text-sm text-slate-800 uppercase tracking-wide mt-6 mb-3">Terms of Offer</h4>
                  <div className="bg-slate-50 rounded-xl p-4 font-sans text-sm border border-slate-100 mb-6">
                    <table className="w-full">
                      <tbody>
                        <tr className="border-b border-slate-200/50">
                          <td className="py-2.5 text-slate-500 font-medium">Designation:</td>
                          <td className="py-2.5 text-slate-900 font-semibold text-right">{candidate.designation}</td>
                        </tr>
                        <tr className="border-b border-slate-200/50">
                          <td className="py-2.5 text-slate-500 font-medium">Annual Compensation:</td>
                          <td className="py-2.5 text-slate-900 font-semibold text-right">{candidate.salaryPackage}</td>
                        </tr>
                        <tr>
                          <td className="py-2.5 text-slate-500 font-medium">Date of Joining:</td>
                          <td className="py-2.5 text-slate-900 font-semibold text-right">{formatDate(candidate.joiningDate)}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  {candidate.companyPolicies && (
                    <>
                      <h4 className="font-sans font-bold text-sm text-slate-800 uppercase tracking-wide mt-6 mb-2">Company Policies & Guidelines</h4>
                      <p className="text-xs text-slate-600 mb-6 whitespace-pre-line leading-relaxed font-sans bg-slate-50 p-4 rounded-xl border border-slate-100">
                        {candidate.companyPolicies}
                      </p>
                    </>
                  )}

                  <p className="mb-8">
                    Please review this offer and accept it online. We are incredibly excited to welcome you and look forward to building the future of recruitment automation together!
                  </p>

                  <div className="border-t border-slate-100 pt-6 mt-8 font-sans flex flex-col items-center">
                    <p className="text-xs text-slate-400">Issued by Human Resources Department</p>
                    <p className="font-bold text-indigo-600 mt-1">ResumeParser Corp</p>
                  </div>
                </div>

                {/* Offer Actions */}
                {candidate.recruitmentStage === 'OFFER_SENT' && (
                  <div className="mt-8 flex flex-col sm:flex-row items-center justify-center gap-4">
                    <button
                      onClick={handleAcceptOffer}
                      className="w-full sm:w-auto px-8 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-700 hover:to-violet-700 text-white font-semibold rounded-xl shadow-lg shadow-indigo-200 transition duration-150 transform active:scale-95 text-center flex items-center justify-center gap-2"
                    >
                      Accept Official Offer
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default OfferStatus;
