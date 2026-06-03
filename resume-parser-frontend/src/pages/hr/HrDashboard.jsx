import { useState, useEffect } from 'react';
import api from '../../services/api';
import { toast } from 'react-toastify';
import { motion, AnimatePresence } from 'framer-motion';
import {
  BiSearch, BiCheck, BiEnvelope, BiPhone, BiBriefcase,
  BiStar, BiUser, BiChevronDown, BiChevronUp, BiX,
  BiBookOpen, BiCodeAlt, BiAward
} from 'react-icons/bi';

const HrDashboard = () => {
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedId, setExpandedId] = useState(null);
  const [activeFilter, setActiveFilter] = useState('all');

  const fetchProfiles = async () => {
    try {
      setLoading(true);
      const response = await api.get('/hr/profiles');
      setProfiles(response.data);
    } catch (error) {
      console.error(error);
      toast.error('Failed to fetch resumes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfiles();
  }, []);

  const toggleShortlist = async (id) => {
    try {
      const response = await api.put(`/hr/profiles/${id}/shortlist`);
      toast.success(response.data.message);
      fetchProfiles();
    } catch (error) {
      console.error(error);
      toast.error('Failed to update status');
    }
  };

  const filteredProfiles = profiles.filter((profile) => {
    const matchesSearch =
      profile.fullName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      profile.email?.toLowerCase().includes(searchTerm.toLowerCase());

    if (activeFilter === 'shortlisted') return matchesSearch && profile.shortlisted;
    if (activeFilter === 'eligible') return matchesSearch && profile.candidateStatus === 'Eligible';
    if (activeFilter === 'not_eligible') return matchesSearch && profile.candidateStatus !== 'Eligible';
    return matchesSearch;
  });

  const stats = {
    total: profiles.length,
    eligible: profiles.filter(p => p.candidateStatus === 'Eligible').length,
    shortlisted: profiles.filter(p => p.shortlisted).length,
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
        <p className="text-slate-500 font-medium text-sm">Loading resumes...</p>
      </div>
    );
  }

  return (
    <div className="space-y-5">

      {/* ── Page Header ── */}
      <div>
        <h1 className="text-xl md:text-2xl font-bold text-slate-900">Resumes Dashboard</h1>
        <p className="text-slate-500 text-sm mt-1">Manage and review all submitted candidate profiles</p>
      </div>

      {/* ── Stats Cards ── */}
      <div className="grid grid-cols-3 gap-3 md:gap-4">
        {[
          { label: 'Total', value: stats.total, color: 'bg-blue-50 text-blue-700 border-blue-100' },
          { label: 'Eligible', value: stats.eligible, color: 'bg-emerald-50 text-emerald-700 border-emerald-100' },
          { label: 'Shortlisted', value: stats.shortlisted, color: 'bg-amber-50 text-amber-700 border-amber-100' },
        ].map((stat) => (
          <div key={stat.label} className={`rounded-xl border p-3 md:p-4 ${stat.color}`}>
            <p className="text-xs font-semibold uppercase tracking-wider opacity-70">{stat.label}</p>
            <p className="text-2xl md:text-3xl font-black mt-1">{stat.value}</p>
          </div>
        ))}
      </div>

      {/* ── Search + Filter Bar ── */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <BiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-lg" />
          <input
            type="text"
            placeholder="Search by name or email..."
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-200 bg-white focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 text-sm transition-all"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          {searchTerm && (
            <button
              onClick={() => setSearchTerm('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              <BiX />
            </button>
          )}
        </div>

        {/* Filter tabs */}
        <div className="flex gap-1.5 overflow-x-auto pb-0.5 flex-shrink-0">
          {[
            { key: 'all', label: 'All' },
            { key: 'eligible', label: 'Eligible' },
            { key: 'shortlisted', label: 'Shortlisted' },
            { key: 'not_eligible', label: 'Not Eligible' },
          ].map((f) => (
            <button
              key={f.key}
              onClick={() => setActiveFilter(f.key)}
              className={`px-3 py-2 rounded-lg text-xs font-semibold whitespace-nowrap transition-all ${
                activeFilter === f.key
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* ── Profile Cards ── */}
      <div className="space-y-3">
        <AnimatePresence>
          {filteredProfiles.map((profile, idx) => (
            <motion.div
              key={profile.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.04 }}
              className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow"
            >
              {/* Card Header */}
              <div className="p-4 md:p-5">
                <div className="flex items-start gap-3">
                  {/* Avatar */}
                  <div className="w-10 h-10 md:w-12 md:h-12 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold text-lg flex-shrink-0">
                    {profile.fullName?.charAt(0)?.toUpperCase() || <BiUser />}
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2 flex-wrap">
                      <div className="min-w-0">
                        <h3 className="font-bold text-slate-900 text-base truncate">{profile.fullName || 'Unknown'}</h3>
                        <div className="flex flex-col sm:flex-row sm:items-center gap-0.5 sm:gap-3 text-xs text-slate-500 mt-0.5">
                          <span className="flex items-center gap-1 truncate">
                            <BiEnvelope className="flex-shrink-0" />
                            <span className="truncate">{profile.email}</span>
                          </span>
                          {profile.phone && (
                            <span className="flex items-center gap-1">
                              <BiPhone className="flex-shrink-0" />
                              {profile.phone}
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Badges */}
                      <div className="flex items-center gap-2 flex-shrink-0">
                        {/* ATS Score */}
                        <div className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                          (profile.atsScore || 0) >= 80
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'bg-red-100 text-red-600'
                        }`}>
                          ATS: {profile.atsScore || 0}%
                        </div>
                        {/* Status */}
                        <div className={`text-xs font-semibold px-2.5 py-1 rounded-full hidden sm:block ${
                          profile.candidateStatus === 'Eligible'
                            ? 'bg-emerald-100 text-emerald-700'
                            : 'bg-red-100 text-red-600'
                        }`}>
                          {profile.candidateStatus || 'N/A'}
                        </div>
                      </div>
                    </div>

                    {/* Action buttons row */}
                    <div className="flex items-center gap-2 mt-3 flex-wrap">
                      <button
                        onClick={() => setExpandedId(expandedId === profile.id ? null : profile.id)}
                        className="flex items-center gap-1.5 px-3 py-1.5 border border-slate-200 hover:bg-slate-50 rounded-lg text-xs font-semibold text-slate-600 transition-colors"
                      >
                        {expandedId === profile.id ? (
                          <><BiChevronUp className="text-base" /> Hide Details</>
                        ) : (
                          <><BiChevronDown className="text-base" /> View Details</>
                        )}
                      </button>

                      <button
                        onClick={() => toggleShortlist(profile.id)}
                        className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                          profile.shortlisted
                            ? 'bg-emerald-100 text-emerald-700 hover:bg-emerald-200'
                            : 'bg-blue-600 text-white hover:bg-blue-700'
                        }`}
                      >
                        {profile.shortlisted
                          ? <><BiCheck className="text-base" /> Shortlisted</>
                          : <><BiStar className="text-base" /> Shortlist</>
                        }
                      </button>

                      {/* Mobile-only status badge */}
                      <div className={`sm:hidden text-xs font-semibold px-2.5 py-1.5 rounded-full ${
                        profile.candidateStatus === 'Eligible'
                          ? 'bg-emerald-100 text-emerald-700'
                          : 'bg-red-100 text-red-600'
                      }`}>
                        {profile.candidateStatus || 'N/A'}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Expanded Details */}
              <AnimatePresence>
                {expandedId === profile.id && (
                  <motion.div
                    initial={{ height: 0, opacity: 0 }}
                    animate={{ height: 'auto', opacity: 1 }}
                    exit={{ height: 0, opacity: 0 }}
                    transition={{ duration: 0.25 }}
                    className="overflow-hidden"
                  >
                    <div className="border-t border-slate-100 bg-slate-50 p-4 md:p-5 grid grid-cols-1 lg:grid-cols-2 gap-5">

                      {/* Left — Experience & Education */}
                      <div className="space-y-4">
                        {/* Experience */}
                        <div>
                          <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                            <BiBriefcase className="text-blue-500" /> Experience
                          </h4>
                          {profile.experience && profile.experience.length > 0 ? (
                            <div className="space-y-2">
                              {profile.experience.map((exp, idx) => (
                                <div key={idx} className="bg-white p-3 rounded-lg border border-slate-100 text-xs">
                                  <p className="font-semibold text-slate-800">{exp.jobTitle}</p>
                                  <p className="text-blue-600 mt-0.5">{exp.company}</p>
                                  <p className="text-slate-400 mt-1">
                                    {exp.startDate || 'N/A'} – {exp.endDate || 'Present'} · {exp.duration}
                                  </p>
                                  {exp.responsibilities && exp.responsibilities.length > 0 && (
                                    <div className="mt-2 text-slate-650 border-t border-slate-100 pt-2">
                                      <p className="font-semibold text-[10px] uppercase tracking-wider text-slate-400 mb-1">Responsibilities:</p>
                                      <ul className="list-disc list-inside space-y-1 text-slate-650">
                                        {exp.responsibilities.map((resp, rIdx) => (
                                          <li key={rIdx} className="pl-1 text-[11px] leading-relaxed">{resp}</li>
                                        ))}
                                      </ul>
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-slate-400 italic">No experience listed.</p>
                          )}
                        </div>

                        {/* Education */}
                        <div>
                          <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                            <BiBookOpen className="text-indigo-500" /> Education
                          </h4>
                          {profile.education && profile.education.length > 0 ? (
                            <div className="space-y-2">
                              {profile.education.map((edu, idx) => (
                                <div key={idx} className="bg-white p-3 rounded-lg border border-slate-100 text-xs">
                                  <p className="font-semibold text-slate-800">{edu.degree}{edu.specialization ? ` – ${edu.specialization}` : ''}</p>
                                  <p className="text-slate-500 mt-0.5">{edu.institution}{edu.graduationYear ? ` (${edu.graduationYear})` : ''}</p>
                                  {edu.cgpa && <p className="text-slate-400 mt-0.5">CGPA: {edu.cgpa}</p>}
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-slate-400 italic">No education listed.</p>
                          )}
                        </div>
                      </div>

                      {/* Right — Skills */}
                      <div className="space-y-4">
                        <div>
                          <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                            <BiCodeAlt className="text-emerald-500" /> Skills
                          </h4>
                          {profile.skills && profile.skills.length > 0 ? (
                            <div className="flex flex-wrap gap-2">
                              {profile.skills.map((skill, idx) => (
                                <span
                                  key={idx}
                                  className="px-2.5 py-1 bg-white border border-blue-100 text-blue-700 text-xs rounded-full shadow-sm font-medium"
                                >
                                  {skill.skillName}
                                </span>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-slate-400 italic">No skills listed.</p>
                          )}
                        </div>

                        {/* Summary */}
                        {profile.professionalSummary && (
                          <div>
                            <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                              <BiAward className="text-amber-500" /> Summary
                            </h4>
                            <p className="text-xs text-slate-500 bg-white p-3 rounded-lg border border-slate-100 leading-relaxed">
                              {profile.professionalSummary}
                            </p>
                          </div>
                        )}

                        {/* Projects */}
                        {profile.projects && profile.projects.length > 0 && (
                          <div>
                            <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                              <BiCodeAlt className="text-blue-500" /> Projects
                            </h4>
                            <div className="space-y-1.5">
                              {profile.projects.map((proj, idx) => (
                                <div key={idx} className="bg-white p-2.5 rounded-lg border border-slate-100 text-xs text-slate-650 leading-relaxed">
                                  {proj}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Certifications */}
                        {profile.certifications && profile.certifications.length > 0 && (
                          <div>
                            <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                              <BiAward className="text-amber-500" /> Certifications
                            </h4>
                            <div className="flex flex-wrap gap-2">
                              {profile.certifications.map((cert, idx) => (
                                <span
                                  key={idx}
                                  className="px-2.5 py-1 bg-white border border-amber-100 text-amber-700 text-xs rounded-full shadow-sm font-medium"
                                >
                                  {cert}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Languages */}
                        {profile.languages && profile.languages.length > 0 && (
                          <div>
                            <h4 className="font-bold text-slate-700 text-sm flex items-center gap-2 mb-2">
                              <BiBookOpen className="text-violet-500" /> Languages
                            </h4>
                            <div className="flex flex-wrap gap-2">
                              {profile.languages.map((lang, idx) => (
                                <span
                                  key={idx}
                                  className="px-2.5 py-1 bg-white border border-violet-100 text-violet-700 text-xs rounded-full shadow-sm font-medium"
                                >
                                  {lang}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </AnimatePresence>

        {filteredProfiles.length === 0 && (
          <div className="text-center py-16 text-slate-400">
            <BiUser className="text-5xl mx-auto mb-3 opacity-30" />
            <p className="font-medium">No resumes found.</p>
            <p className="text-sm mt-1 opacity-70">Try adjusting the search or filter.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default HrDashboard;
