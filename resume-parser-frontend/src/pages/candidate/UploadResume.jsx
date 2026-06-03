import { useState, useRef, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { BiUser, BiEnvelope, BiPhone, BiCloudUpload, BiFile, BiCheck, BiX, BiCheckCircle, BiErrorCircle } from 'react-icons/bi';
import { toast } from 'react-toastify';
import { AuthContext } from '../../context/AuthContext';
import api from '../../services/api';
import './UploadResume.css';

const UploadResume = () => {
  const [file, setFile] = useState(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0); // 0 to 6
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    jobDescription: ''
  });
  const [result, setResult] = useState(null);
  const fileInputRef = useRef(null);
  
  const { user, isAuthenticated, logout } = useContext(AuthContext);
  const navigate = useNavigate();

  // Initialize theme state from localStorage
  const [isDark, setIsDark] = useState(() => {
    const stored = localStorage.getItem('theme');
    if (stored) return stored === 'dark';
    return document.documentElement.classList.contains('dark') || 
           document.documentElement.getAttribute('data-theme') !== 'light';
  });

  useEffect(() => {
    const root = document.documentElement;
    if (isDark) {
      root.classList.add('dark');
      root.classList.remove('light');
      root.setAttribute('data-theme', 'dark');
    } else {
      root.classList.remove('dark');
      root.classList.add('light');
      root.setAttribute('data-theme', 'light');
    }
    localStorage.setItem('theme', isDark ? 'dark' : 'light');
  }, [isDark]);

  const steps = [
    "Uploading Resume...",
    "Extracting Information...",
    "Parsing Education...",
    "Analyzing Experience...",
    "Identifying Skills...",
    "Saving Profile..."
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    if (name === 'phone') {
      // Only allow digits and restrict to maximum of 10 digits
      const digitsOnly = value.replace(/\D/g, '');
      if (digitsOnly.length <= 10) {
        setFormData(prev => ({ ...prev, [name]: digitsOnly }));
      }
      return;
    }

    if (name === 'fullName') {
      // Only allow letters and spaces
      const lettersOnly = value.replace(/[^a-zA-Z\s]/g, '');
      setFormData(prev => ({ ...prev, [name]: lettersOnly }));
      return;
    }

    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      validateAndSetFile(e.target.files[0]);
    }
  };

  const validateAndSetFile = (selectedFile) => {
    if (selectedFile.size > 5 * 1024 * 1024) {
      toast.error('File size should be less than 5MB');
      return;
    }
    const validTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    if (!validTypes.includes(selectedFile.type) && !selectedFile.name.match(/\.(pdf|doc|docx)$/i)) {
      toast.error('Please upload a PDF, DOC, or DOCX file');
      return;
    }
    setFile(selectedFile);
  };

  const handleDragEnter = (e) => { e.preventDefault(); e.stopPropagation(); setIsDragActive(true); };
  const handleDragLeave = (e) => { e.preventDefault(); e.stopPropagation(); setIsDragActive(false); };
  const handleDragOver = (e) => { e.preventDefault(); e.stopPropagation(); setIsDragActive(true); };
  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndSetFile(e.dataTransfer.files[0]);
    }
  };

  const simulateProgress = () => {
    return new Promise(resolve => {
      let currentStep = 0;
      const interval = setInterval(() => {
        currentStep++;
        setUploadProgress(currentStep);
        if (currentStep >= steps.length) {
          clearInterval(interval);
          resolve();
        }
      }, 800);
    });
  };

  const handleUpload = async () => {
    if (!file) {
      toast.warning('Please select a file first');
      return;
    }

    if (formData.fullName) {
      const nameRegex = /^[a-zA-Z\s]+$/;
      if (!nameRegex.test(formData.fullName)) {
        toast.error('Full Name must contain only letters and spaces');
        return;
      }
    }

    if (formData.phone) {
      const phoneRegex = /^\d{10}$/;
      if (!phoneRegex.test(formData.phone)) {
        toast.error('Phone number must be exactly 10 digits');
        return;
      }
    }
    
    setIsUploading(true);
    setUploadProgress(0);
    
    try {
      const uploadData = new FormData();
      uploadData.append('file', file);
      if (formData.fullName) uploadData.append('fullName', formData.fullName);
      if (formData.email) uploadData.append('email', formData.email);
      if (formData.phone) uploadData.append('phone', formData.phone);
      if (formData.jobDescription) uploadData.append('jobDescription', formData.jobDescription);

      const uploadPromise = api.post('/candidate/upload-resume', uploadData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      const response = await Promise.all([simulateProgress(), uploadPromise]);
      const resultData = response[1].data;
      
      toast.success('Resume uploaded successfully!');
      setFile(null);
      setFormData({ fullName: '', email: '', phone: '', jobDescription: '' });
      setUploadProgress(0);
      setResult(resultData);
    } catch (error) {
      console.error(error);
      const errorMsg = error.response?.data?.message || 'Failed to upload resume. Please try again.';
      toast.error(errorMsg);
      setUploadProgress(0);
    } finally {
      setIsUploading(false);
    }
  };

  const handleLogout = () => {
    logout();
    toast.success('Logged out successfully');
    navigate('/login');
  };

  return (
    <div className="upload-resume-page">
      {/* ── NAVIGATION ── */}
      <nav>
        <div className="nav-logo">
          <div className="logo-mark">⚡</div>
          RecruitAI
        </div>
        <div className="nav-badge">AI Engine v3.0</div>
        <div className="nav-right">
          <button 
            className="theme-toggle" 
            onClick={() => setIsDark(!isDark)} 
            aria-label="Toggle theme"
          >
            <span className="toggle-sun">☀️</span>
            <span style={{ fontSize: '11px', fontWeight: 500 }}>
              {isDark ? 'Light' : 'Dark'}
            </span>
            <span className="toggle-moon">🌙</span>
          </button>
          
          {isAuthenticated ? (
            <>
              <button 
                className="btn-ghost" 
                onClick={() => {
                  if (user?.role === 'ADMIN') navigate('/admin/dashboard');
                  else if (user?.role === 'HR') navigate('/hr/dashboard');
                  else navigate('/upload-resume');
                }}
              >
                Dashboard
              </button>
              <button className="btn-primary-sm" onClick={handleLogout}>
                Sign Out
              </button>
            </>
          ) : (
            <>
              <button className="btn-ghost" onClick={() => navigate('/login')}>Sign In</button>
              <button className="btn-primary-sm" onClick={() => navigate('/login')}>Get Started Free</button>
            </>
          )}
        </div>
      </nav>

      <div className="wrapper">
        {/* ── LEFT HERO ── */}
        <div className="hero">
          <div className="hero-eyebrow">
            <span className="eyebrow-dot"></span>
            Intelligent Screening Platform
          </div>
          <h1>AI-Powered<br />Resume<br /><span>Screening</span></h1>
          <p className="hero-desc">
            Extract key professional metrics, evaluate ATS compatibility scores, and dynamically route candidate profiles — all in seconds.
          </p>
          <div className="feature-grid">
            <div className="feature-pill">
              <div className="fp-icon purple">⚡</div>
              <div>
                <div className="fp-label">Instant Parsing</div>
                <div className="fp-sub">Automatic text extraction</div>
              </div>
            </div>
            <div className="feature-pill">
              <div className="fp-icon gold">🎯</div>
              <div>
                <div className="fp-label">ATS Scoring</div>
                <div className="fp-sub">Keyword match analysis</div>
              </div>
            </div>
            <div className="feature-pill">
              <div className="fp-icon green">🔀</div>
              <div>
                <div className="fp-label">Smart Routing</div>
                <div className="fp-sub">Dynamic algorithms</div>
              </div>
            </div>
            <div className="feature-pill">
              <div className="fp-icon pink">📡</div>
              <div>
                <div className="fp-label">Real-time Status</div>
                <div className="fp-sub">Live pipeline tracking</div>
              </div>
            </div>
          </div>
          
          <div className="stats-row">
            <div>
              <div className="stat-num">98.4%</div>
              <div className="stat-label">Parse Accuracy</div>
            </div>
            <div className="stat-div"></div>
            <div>
              <div className="stat-num">2.1s</div>
              <div className="stat-label">Avg. Screen Time</div>
            </div>
            <div className="stat-div"></div>
            <div>
              <div className="stat-num">500K+</div>
              <div className="stat-label">Profiles Processed</div>
            </div>
          </div>
        </div>

        {/* ── RIGHT FORM CARD ── */}
        <div className="card-panel">
          <div className="upload-card">
            
            {result ? (
              /* ── RESULT DASHBOARD PANEL ── */
              <div className="card-body">
                <div className="result-pane">
                  <div className="result-badge-wrap">
                    {result.Status === 'Eligible' ? (
                      <div className="result-badge eligible">
                        <BiCheckCircle size={14} style={{ marginRight: '4px' }} /> Candidate Match Verified
                      </div>
                    ) : (
                      <div className="result-badge ineligible">
                        <BiErrorCircle size={14} style={{ marginRight: '4px' }} /> Below Evaluation Threshold
                      </div>
                    )}
                  </div>
                  
                  <h2 className="result-title">Screening Report</h2>
                  
                  <div className="result-grid">
                    {/* Radial ATS Gauge */}
                    <div className="result-radial-container">
                      <div className="radial-svg-wrap">
                        <svg className="w-full h-full transform -rotate-90" viewBox="0 0 144 144">
                          <circle
                            cx="72"
                            cy="72"
                            r="60"
                            stroke="rgba(255,255,255,0.03)"
                            strokeWidth="8"
                            fill="transparent"
                          />
                          <circle
                            cx="72"
                            cy="72"
                            r="60"
                            stroke={result.Status === 'Eligible' ? 'var(--success)' : '#F05078'}
                            strokeWidth="8"
                            fill="transparent"
                            strokeDasharray={377}
                            strokeDashoffset={377 - (377 * (result.ATSScore || 0)) / 100}
                            strokeLinecap="round"
                            style={{ transition: 'stroke-dashoffset 1.2s ease-out' }}
                          />
                        </svg>
                        <div className="radial-svg-text">
                          <span className="radial-score">{result.ATSScore || 0}%</span>
                          <span className="radial-label">Score</span>
                        </div>
                      </div>
                      <div className="radial-title-desc">ATS Compatibility</div>
                    </div>
                    
                    {/* Details Table */}
                    <div className="result-details">
                      <div className="detail-row">
                        <span className="detail-row-label">Full Name</span>
                        <span className="detail-row-value">{result.FullName || 'N/A'}</span>
                      </div>
                      <div className="detail-row">
                        <span className="detail-row-label">Email Address</span>
                        <span className="detail-row-value">{result.Email || 'N/A'}</span>
                      </div>
                      {result.PhoneNumber && (
                        <div className="detail-row">
                          <span className="detail-row-label">Phone</span>
                          <span className="detail-row-value">{result.PhoneNumber}</span>
                        </div>
                      )}
                      {result.Location && (
                        <div className="detail-row">
                          <span className="detail-row-label">Location</span>
                          <span className="detail-row-value">{result.Location}</span>
                        </div>
                      )}
                      <div className="detail-row">
                        <span className="detail-row-label">Status</span>
                        <span 
                          className="detail-row-value status-text" 
                          style={{ color: result.Status === 'Eligible' ? 'var(--success)' : '#F05078' }}
                        >
                          {result.Status || 'N/A'}
                        </span>
                      </div>
                      <div className="detail-row">
                        <span className="detail-row-label">Destination</span>
                        <span className="detail-row-value dest-badge">{result.StoredIn || 'N/A'}</span>
                      </div>
                    </div>
                  </div>

                  {/* ── ATS FEEDBACK CARD ── */}
                  <div className="feedback-card">
                    <div className="feedback-card-header">
                      {result.Status === 'Eligible' ? (
                        <>🎉 Qualification Verdict</>
                      ) : (
                        <>⚠️ Rejection Verdict & Recommendations</>
                      )}
                    </div>
                    <div className="feedback-card-body">
                      {result.FeedbackReason || 'Resume evaluation process completed.'}
                    </div>
                  </div>

                  {/* ── SKILLS COMPARISON ── */}
                  <div className="skills-comparison-grid">
                    <div className="skills-box">
                      <div className="skills-box-title">✅ Matching Skills</div>
                      <div className="skills-box-tags">
                        {result.MatchingSkills && result.MatchingSkills.length > 0 ? (
                          result.MatchingSkills.map((skill, index) => (
                            <span key={index} className="skill-tag match">{skill}</span>
                          ))
                        ) : (
                          <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>None identified</span>
                        )}
                      </div>
                    </div>
                    <div className="skills-box">
                      <div className="skills-box-title">❌ Missing Keywords</div>
                      <div className="skills-box-tags">
                        {result.MissingSkills && result.MissingSkills.length > 0 ? (
                          result.MissingSkills.map((skill, index) => (
                            <span key={index} className="skill-tag missing">{skill}</span>
                          ))
                        ) : (
                          <span style={{ fontSize: '11px', color: 'var(--success)' }}>None missing</span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* ── STRENGTHS & AREAS FOR IMPROVEMENT ── */}
                  <div className="audit-lists-grid">
                    <div className="audit-box">
                      <div className="audit-box-title">💪 Resume Strengths</div>
                      <div className="audit-items">
                        {result.Strengths && result.Strengths.length > 0 ? (
                          result.Strengths.map((str, index) => (
                            <div key={index} className="audit-item">
                              <span className="audit-icon-success">✓</span>
                              <span>{str}</span>
                            </div>
                          ))
                        ) : (
                          <div className="audit-item">
                            <span className="audit-icon-success">✓</span>
                            <span>Standard layout metrics satisfied</span>
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="audit-box">
                      <div className="audit-box-title">📈 Areas for Improvement</div>
                      <div className="audit-items">
                        {result.Improvements && result.Improvements.length > 0 ? (
                          result.Improvements.map((imp, index) => (
                            <div key={index} className="audit-item">
                              <span className="audit-icon-warning">!</span>
                              <span>{imp}</span>
                            </div>
                          ))
                        ) : (
                          <div className="audit-item">
                            <span className="audit-icon-warning">!</span>
                            <span>Optimal parsing metrics aligned</span>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                  
                  <button
                    onClick={() => setResult(null)}
                    className="cta-btn"
                    style={{ background: 'var(--bg-glass)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
                  >
                    Analyze Another Resume
                  </button>
                </div>
              </div>
            ) : (
              /* ── UPLOAD PROFILE FORM ── */
              <>
                <div className="card-header">
                  <div>
                    <h2>Upload Profile</h2>
                    <p>Verify your details to start the parsing pipeline</p>
                  </div>
                  <div className="status-chip">
                    <span className="status-dot"></span>
                    Live
                  </div>
                </div>
                <div className="card-body">
                  <div className="form-row single">
                    <div className="field">
                      <label>Full Name</label>
                      <div className="input-icon-wrap">
                        <span className="icon">
                          <BiUser size={16} />
                        </span>
                        <input 
                          type="text" 
                          name="fullName"
                          value={formData.fullName}
                          onChange={handleInputChange}
                          placeholder="e.g. Aisha Patel"
                          disabled={isUploading}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="field">
                      <label>Email Address</label>
                      <div className="input-icon-wrap">
                        <span className="icon">
                          <BiEnvelope size={16} />
                        </span>
                        <input 
                          type="email" 
                          name="email"
                          value={formData.email}
                          onChange={handleInputChange}
                          placeholder="you@company.com"
                          disabled={isUploading}
                        />
                      </div>
                    </div>
                    <div className="field">
                      <label>Phone Number</label>
                      <div className="input-icon-wrap">
                        <span className="icon">
                          <BiPhone size={16} />
                        </span>
                        <input 
                          type="tel" 
                          name="phone"
                          value={formData.phone}
                          onChange={handleInputChange}
                          placeholder="+91 98765 43210"
                          disabled={isUploading}
                        />
                      </div>
                    </div>
                  </div>

                  <div className="form-row single">
                    <div className="field">
                      <label>
                        Job Description / Target Role{' '}
                        <span style={{ fontSize: '10px', letterSpacing: 0, textTransform: 'none', color: 'var(--text-muted)' }}>
                          (Optional)
                        </span>
                      </label>
                      <textarea 
                        name="jobDescription"
                        value={formData.jobDescription}
                        onChange={handleInputChange}
                        placeholder="Paste a job description or keywords here to check compatibility score..."
                        disabled={isUploading}
                      />
                    </div>
                  </div>

                  {/* Drag and Drop Zone */}
                  {!file ? (
                    <div 
                      className={`drop-zone ${isDragActive ? 'drag-active' : ''}`}
                      onClick={() => !isUploading && fileInputRef.current?.click()}
                      onDragEnter={handleDragEnter}
                      onDragLeave={handleDragLeave}
                      onDragOver={handleDragOver}
                      onDrop={handleDrop}
                      style={{ cursor: isUploading ? 'not-allowed' : 'pointer' }}
                    >
                      <input 
                        type="file" 
                        ref={fileInputRef} 
                        accept=".pdf,.doc,.docx" 
                        style={{ display: 'none' }} 
                        onChange={handleFileChange}
                        disabled={isUploading}
                      />
                      <div className="drop-icon">
                        <BiCloudUpload size={24} />
                      </div>
                      <p className="drop-main">
                        Drag & Drop Resume Here <span className="drop-browse">or Browse Files</span>
                      </p>
                      <p className="drop-sub">PDF, DOC, DOCX · Up to 5MB</p>
                    </div>
                  ) : (
                    <div 
                      className="drop-zone" 
                      style={{ borderColor: 'var(--success)', background: 'var(--drop-hover-bg)', position: 'relative' }}
                    >
                      <div className="drop-icon" style={{ color: 'var(--success)', background: 'rgba(61,219,164,0.1)', borderColor: 'rgba(61,219,164,0.2)' }}>
                        <BiFile size={22} />
                      </div>
                      <p className="drop-main">
                        ✅ <strong>{file.name}</strong>
                      </p>
                      <p className="drop-sub">
                        {(file.size / (1024 * 1024)).toFixed(2)} MB · File ready for screening
                      </p>
                      {!isUploading && (
                        <button
                          type="button"
                          onClick={(e) => { e.stopPropagation(); setFile(null); }}
                          style={{
                            position: 'absolute', top: 12, right: 12,
                            background: 'none', border: 'none', color: 'var(--text-muted)',
                            cursor: 'pointer'
                          }}
                        >
                          <BiX size={20} />
                        </button>
                      )}
                    </div>
                  )}

                  {/* AI Parsing Pipeline Progress Box */}
                  {isUploading && (
                    <div className="pipeline-box">
                      <div className="pipeline-header">
                        <span>AI Parsing Pipeline</span>
                        <span className="pipeline-percentage">
                          {Math.round((uploadProgress / steps.length) * 100)}%
                        </span>
                      </div>
                      
                      <div className="pipeline-progress-bar">
                        <div 
                          className="pipeline-progress-fill" 
                          style={{ width: `${(uploadProgress / steps.length) * 100}%` }}
                        />
                      </div>

                      <div className="pipeline-steps">
                        {steps.map((step, idx) => {
                          const isActive = uploadProgress === idx;
                          const isCompleted = uploadProgress > idx;

                          return (
                            <div 
                              key={idx} 
                              className={`pipeline-step-item ${isCompleted ? 'completed' : isActive ? 'active' : ''}`}
                            >
                              <div className="pipeline-dot-indicator">
                                {isCompleted ? <BiCheck size={8} /> : isActive ? '•' : ''}
                              </div>
                              <span>{step}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}

                  <button 
                    className="cta-btn" 
                    onClick={handleUpload}
                    disabled={!file || isUploading}
                  >
                    <div className="cta-shine"></div>
                    {isUploading ? (
                      <>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 1s linear infinite' }}>
                          <path d="M12 2a10 10 0 0110 10" />
                        </svg>
                        Analysing Profile...
                      </>
                    ) : (
                      <>
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <circle cx="11" cy="11" r="7" />
                          <path d="M21 21l-4.35-4.35" />
                          <path d="M8 11h6M11 8v6" strokeWidth="2.5" />
                        </svg>
                        Screen Resume Profile
                      </>
                    )}
                  </button>

                  <div className="trust-row">
                    <div className="trust-item">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="11" width="18" height="10" rx="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>
                      <span>Secure Storage</span>
                    </div>
                    <span className="trust-dot">·</span>
                    <div className="trust-item">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 2L3 7v5c0 5.25 3.75 10.15 9 11.35C17.25 22.15 21 17.25 21 12V7l-9-5z"/></svg>
                      <span>Data Encryption</span>
                    </div>
                    <span className="trust-dot">·</span>
                    <div className="trust-item">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="9 11 12 14 22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
                      <span>ATS Match System</span>
                    </div>
                  </div>
                </div>
              </>
            )}

          </div>
        </div>
      </div>
    </div>
  );
};

export default UploadResume;
