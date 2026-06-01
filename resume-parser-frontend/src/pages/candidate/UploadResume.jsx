import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { BiUser, BiEnvelope, BiPhone, BiCloudUpload, BiFile, BiCheck, BiX, BiBrain, BiTargetLock, BiBarChartAlt2, BiTimeFive, BiLockAlt, BiCheckShield, BiChip, BiCheckCircle, BiErrorCircle } from 'react-icons/bi';
import { toast } from 'react-toastify';
import api from '../../services/api';

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
        setFormData({ ...formData, [name]: digitsOnly });
      }
      return;
    }

    if (name === 'fullName') {
      // Only allow letters and spaces
      const lettersOnly = value.replace(/[^a-zA-Z\s]/g, '');
      setFormData({ ...formData, [name]: lettersOnly });
      return;
    }

    setFormData({ ...formData, [name]: value });
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
        headers: { 'Content-Type': undefined }
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

  return (
    <div className="min-h-screen flex w-full bg-slate-950 overflow-hidden relative font-sans text-slate-100">
      
      {/* Background Animated Gradient Mesh */}
      <div className="absolute inset-0 overflow-hidden">
        <motion.div
          animate={{
            scale: [1, 1.15, 1],
            x: [0, 60, 0],
            y: [0, -40, 0],
          }}
          transition={{
            duration: 20,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className="absolute -top-40 -left-40 w-[600px] h-[600px] bg-blue-600/10 rounded-full blur-[140px] pointer-events-none"
        />
        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            x: [0, -50, 0],
            y: [0, 60, 0],
          }}
          transition={{
            duration: 25,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className="absolute -bottom-40 -right-40 w-[600px] h-[600px] bg-indigo-600/10 rounded-full blur-[140px] pointer-events-none"
        />
        <div className="absolute top-1/3 right-1/4 w-[400px] h-[400px] bg-violet-600/5 rounded-full blur-[160px] pointer-events-none" />
      </div>
      
      {/* Container */}
      <div className="flex flex-col lg:flex-row w-full max-w-[1600px] mx-auto z-10 p-4 lg:p-8 relative">
        
        {/* Left Side (40%) */}
        <div className="w-full lg:w-[40%] flex flex-col justify-center p-6 lg:p-12 mb-8 lg:mb-0">
          <motion.div 
            initial={{ opacity: 0, x: -30 }} 
            animate={{ opacity: 1, x: 0 }} 
            transition={{ duration: 0.6 }}
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 text-xs font-semibold mb-6 border border-indigo-500/20 shadow-inner">
              <BiChip className="text-sm" /> AI Engine v2.0
            </div>
            
            <h1 className="text-4xl lg:text-5xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-white via-slate-100 to-slate-300 leading-tight mb-6">
              AI-Powered <br className="hidden lg:block"/> Resume <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-400 via-indigo-400 to-purple-500">Screening</span>
            </h1>
            
            <p className="text-base text-slate-400 mb-10 leading-relaxed max-w-lg">
              Upload your resume to extract key professional metrics, evaluate ATS compatibility scores, and route your profile dynamically based on eligibility.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-xl">
              {[
                { icon: <BiBrain className="text-xl text-blue-400 animate-pulse" />, title: "Instant Parsing", desc: "Automatic field extraction" },
                { icon: <BiTargetLock className="text-xl text-indigo-400" />, title: "ATS Scoring", desc: "Weighted exact keyword match" },
                { icon: <BiBarChartAlt2 className="text-xl text-emerald-400" />, title: "Smart Routing", desc: "Eligible matching algorithms" },
                { icon: <BiTimeFive className="text-xl text-violet-400" />, title: "Real-time Status", desc: "Instant database screening" }
              ].map((feature, idx) => (
                <motion.div 
                  key={idx}
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.2 + (idx * 0.08), duration: 0.4 }}
                  className="flex items-start gap-3 p-4 rounded-xl bg-slate-900/30 border border-slate-800/40 backdrop-blur-sm hover:border-slate-800 transition-colors"
                >
                  <div className="p-2 bg-slate-950/60 shadow-inner rounded-lg flex-shrink-0 border border-slate-800">
                    {feature.icon}
                  </div>
                  <div>
                    <h3 className="font-semibold text-slate-200 text-sm">{feature.title}</h3>
                    <p className="text-xs text-slate-500 mt-0.5 leading-relaxed">{feature.desc}</p>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </div>

        {/* Right Side (60%) */}
        <div className="w-full lg:w-[60%] flex items-center justify-center p-4 lg:p-8">
          <motion.div 
            initial={{ opacity: 0, y: 30 }} 
            animate={{ opacity: 1, y: 0 }} 
            transition={{ duration: 0.6, delay: 0.2 }}
            className="w-full max-w-[700px] relative"
          >
            {/* Ambient card background glow */}
            <div className="absolute -inset-0.5 bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-600 rounded-3xl blur-xl opacity-20 pointer-events-none"></div>

            <div className="relative w-full bg-slate-900/60 backdrop-blur-2xl border border-slate-800/80 shadow-[0_0_50px_rgba(0,0,0,0.3)] rounded-3xl overflow-hidden p-8 lg:p-10">
              
              <AnimatePresence mode="wait">
                {result ? (
                  <motion.div
                    key="result-pane"
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    className="space-y-8"
                  >
                    {/* Result Header Badge */}
                    <div className="text-center">
                      <motion.div 
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full text-sm font-semibold mb-6 shadow-inner border border-slate-850"
                        style={{
                          backgroundColor: result.Status === 'Eligible' ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)',
                          color: result.Status === 'Eligible' ? '#10b981' : '#ef4444',
                          borderColor: result.Status === 'Eligible' ? 'rgba(16,185,129,0.2)' : 'rgba(239,68,68,0.2)'
                        }}
                      >
                        {result.Status === 'Eligible' ? (
                          <>
                            <BiCheckCircle className="text-lg animate-bounce" />
                            <span>Candidate Match Verified</span>
                          </>
                        ) : (
                          <>
                            <BiErrorCircle className="text-lg" />
                            <span>Below Evaluation Threshold</span>
                          </>
                        )}
                      </motion.div>
                      
                      <h2 className="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-white via-slate-100 to-slate-300 tracking-tight">
                        Screening Report
                      </h2>
                    </div>

                    {/* Dashboard Metrics Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
                      
                      {/* ATS Radial Progress Gauge */}
                      <div className="md:col-span-5 flex flex-col items-center justify-center p-6 bg-slate-950/40 rounded-2xl border border-slate-850/80 shadow-inner">
                        <div className="relative w-36 h-36 flex items-center justify-center">
                          {/* Radial Progress Circle SVG */}
                          <svg className="w-full h-full transform -rotate-90">
                            <circle
                              cx="72"
                              cy="72"
                              r="60"
                              stroke="rgba(30,41,59,0.5)"
                              strokeWidth="8"
                              fill="transparent"
                            />
                            <motion.circle
                              cx="72"
                              cy="72"
                              r="60"
                              stroke={result.Status === 'Eligible' ? '#10b981' : '#ef4444'}
                              strokeWidth="8"
                              fill="transparent"
                              strokeDasharray={377}
                              initial={{ strokeDashoffset: 377 }}
                              animate={{ strokeDashoffset: 377 - (377 * result.ATSScore) / 100 }}
                              transition={{ duration: 1.2, ease: "easeOut" }}
                              strokeLinecap="round"
                            />
                          </svg>
                          <div className="absolute flex flex-col items-center justify-center">
                            <span className="text-4xl font-black text-white tracking-tight">{result.ATSScore}%</span>
                            <span className="text-[10px] font-bold text-slate-500 tracking-widest uppercase mt-0.5">Score</span>
                          </div>
                        </div>
                        <div className="mt-4 text-center">
                          <p className="text-xs text-slate-400 font-semibold uppercase tracking-wider">ATS Score Compatibility</p>
                        </div>
                      </div>

                      {/* Detail Table Card */}
                      <div className="md:col-span-7 bg-slate-950/40 border border-slate-850/80 rounded-2xl p-5 shadow-inner space-y-4">
                        <div className="flex justify-between items-center border-b border-slate-900 pb-3">
                          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">FullName</span>
                          <span className="text-sm font-bold text-white">{result.FullName}</span>
                        </div>
                        <div className="flex justify-between items-center border-b border-slate-900 pb-3">
                          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Email</span>
                          <span className="text-sm font-semibold text-slate-200">{result.Email}</span>
                        </div>
                        {result.PhoneNumber && (
                          <div className="flex justify-between items-center border-b border-slate-900 pb-3">
                            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Phone</span>
                            <span className="text-sm font-semibold text-slate-200">{result.PhoneNumber}</span>
                          </div>
                        )}
                        {result.Location && (
                          <div className="flex justify-between items-center border-b border-slate-900 pb-3">
                            <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Location</span>
                            <span className="text-sm font-semibold text-slate-200">{result.Location}</span>
                          </div>
                        )}
                        <div className="flex justify-between items-center border-b border-slate-900 pb-3">
                          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Status</span>
                          <span className={`text-sm font-black ${result.Status === 'Eligible' ? 'text-emerald-400' : 'text-red-400'}`}>{result.Status}</span>
                        </div>
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-semibold uppercase tracking-wider text-slate-500">Destination</span>
                          <span className="text-xs font-bold font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">{result.StoredIn}</span>
                        </div>
                      </div>
                    </div>

                    {/* Reset Button */}
                    <button
                      onClick={() => setResult(null)}
                      className="w-full h-[54px] rounded-xl font-bold text-white bg-slate-950 border border-slate-850 hover:bg-slate-900 active:scale-[0.98] transition-all flex items-center justify-center gap-2 cursor-pointer shadow-inner"
                    >
                      Analyze Another Resume
                    </button>
                  </motion.div>
                ) : (
                  <motion.div
                    key="form-pane"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="space-y-8"
                  >
                    {/* Header */}
                    <div>
                      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-500/10 text-blue-400 text-xs font-semibold mb-4 border border-blue-500/20 shadow-inner">
                        👋 Welcome Candidate
                      </span>
                      <h2 className="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-white via-slate-100 to-slate-300 tracking-tight mb-2">Upload Profile</h2>
                      <p className="text-slate-400 text-sm leading-relaxed">
                        Verify your details and upload a standard PDF, DOC, or DOCX resume to start the parsing pipeline.
                      </p>
                    </div>

                    {/* Form Fields */}
                    <div className="space-y-6">
                      
                      {/* Full Name Input */}
                      <div className="relative group space-y-2">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Full Name</label>
                        <div className="relative">
                          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-400 transition-colors">
                            <BiUser size={18} />
                          </span>
                          <input
                            type="text"
                            name="fullName"
                            required
                            value={formData.fullName}
                            onChange={handleInputChange}
                            className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 rounded-xl text-white placeholder-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all text-sm shadow-inner"
                            placeholder="e.g. John Doe"
                          />
                        </div>
                      </div>

                      {/* Email & Phone */}
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                        {/* Email */}
                        <div className="relative group space-y-2">
                          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Email Address</label>
                          <div className="relative">
                            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-400 transition-colors">
                              <BiEnvelope size={18} />
                            </span>
                            <input
                              type="email"
                              name="email"
                              required
                              value={formData.email}
                              onChange={handleInputChange}
                              className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 rounded-xl text-white placeholder-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all text-sm shadow-inner"
                              placeholder="john.doe@example.com"
                            />
                          </div>
                        </div>

                        {/* Phone */}
                        <div className="relative group space-y-2">
                          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Phone Number</label>
                          <div className="relative">
                            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 group-focus-within:text-blue-400 transition-colors">
                              <BiPhone size={18} />
                            </span>
                            <input
                              type="tel"
                              name="phone"
                              required
                              value={formData.phone}
                              onChange={handleInputChange}
                              maxLength="10"
                              className="w-full pl-10 pr-4 py-3 bg-slate-950/40 border border-slate-800 rounded-xl text-white placeholder-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all text-sm shadow-inner"
                              placeholder="10-digit number"
                            />
                          </div>
                        </div>
                      </div>

                      {/* Job Description Textarea */}
                      <div className="relative group space-y-2">
                        <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">Job Description / Target Role (Optional)</label>
                        <textarea
                          name="jobDescription"
                          value={formData.jobDescription}
                          onChange={handleInputChange}
                          rows="3"
                          className="w-full p-4 bg-slate-950/40 border border-slate-800 rounded-xl text-white placeholder-slate-600 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all text-sm shadow-inner resize-none leading-relaxed"
                          placeholder="Paste a job description or keywords here to check compatibility score..."
                        />
                      </div>

                      {/* File Upload Zone */}
                      {!file ? (
                        <motion.div
                          whileHover={{ scale: 1.005 }}
                          whileTap={{ scale: 0.995 }}
                          onDragEnter={handleDragEnter}
                          onDragLeave={handleDragLeave}
                          onDragOver={handleDragOver}
                          onDrop={handleDrop}
                          onClick={() => fileInputRef.current?.click()}
                          className={`relative w-full h-44 border-2 border-dashed rounded-2xl flex flex-col items-center justify-center cursor-pointer transition-all duration-300 ${
                            isDragActive 
                              ? 'border-indigo-500 bg-indigo-500/10' 
                              : 'border-slate-800 bg-slate-950/40 hover:border-slate-700 hover:bg-slate-950/60 shadow-inner'
                          }`}
                        >
                          <input
                            type="file"
                            ref={fileInputRef}
                            className="hidden"
                            accept=".pdf,.doc,.docx"
                            onChange={handleFileChange}
                          />
                          <motion.div 
                            animate={isDragActive ? { y: -5, scale: 1.1 } : { y: 0, scale: 1 }}
                            className="w-14 h-14 mb-3 rounded-full bg-gradient-to-br from-indigo-500/10 to-blue-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 shadow-inner"
                          >
                            <BiCloudUpload size={28} />
                          </motion.div>
                          <p className="text-slate-200 font-semibold text-sm mb-1">
                            Drag & Drop Resume Here
                          </p>
                          <p className="text-slate-500 text-xs">or <span className="text-indigo-400 font-medium hover:underline">Browse Files</span></p>
                          <p className="text-[10px] text-slate-600 mt-3 font-semibold uppercase tracking-wider">PDF, DOC, DOCX up to 5MB</p>
                        </motion.div>
                      ) : (
                        <motion.div 
                          initial={{ opacity: 0, scale: 0.98 }}
                          animate={{ opacity: 1, scale: 1 }}
                          className="w-full p-4 border border-emerald-500/20 bg-emerald-500/5 rounded-xl flex items-center justify-between shadow-inner"
                        >
                          <div className="flex items-center gap-3 overflow-hidden">
                            <div className="w-10 h-10 rounded-lg bg-emerald-500/10 border border-emerald-500/25 flex items-center justify-center text-emerald-400 flex-shrink-0">
                              <BiFile size={20} />
                            </div>
                            <div className="truncate">
                              <p className="font-semibold text-slate-200 text-sm truncate">{file.name}</p>
                              <p className="text-xs text-slate-500 mt-0.5">{(file.size / (1024 * 1024)).toFixed(2)} MB</p>
                            </div>
                          </div>
                          <button
                            onClick={(e) => { e.stopPropagation(); setFile(null); }}
                            className="p-2 text-slate-500 hover:text-red-400 hover:bg-slate-900 rounded-lg transition-colors ml-2 flex-shrink-0"
                            disabled={isUploading}
                          >
                            <BiX size={20} />
                          </button>
                        </motion.div>
                      )}

                      {/* Progress Experience */}
                      <AnimatePresence>
                        {isUploading && (
                          <motion.div 
                            initial={{ opacity: 0, height: 0 }}
                            animate={{ opacity: 1, height: 'auto' }}
                            exit={{ opacity: 0, height: 0 }}
                            className="bg-slate-950/80 rounded-xl p-5 border border-slate-850 shadow-inner overflow-hidden space-y-4"
                          >
                            <div className="flex justify-between items-center">
                              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">AI Parsing Pipeline</span>
                              <span className="text-sm font-black text-indigo-400">{Math.round((uploadProgress / steps.length) * 100)}%</span>
                            </div>
                            
                            {/* Progress Bar */}
                            <div className="w-full h-1.5 bg-slate-900 rounded-full overflow-hidden">
                              <motion.div 
                                className="h-full bg-gradient-to-r from-blue-500 via-indigo-500 to-purple-600"
                                initial={{ width: '0%' }}
                                animate={{ width: `${(uploadProgress / steps.length) * 100}%` }}
                                transition={{ duration: 0.3 }}
                              />
                            </div>

                            {/* Steps List */}
                            <div className="grid grid-cols-2 gap-2.5 pt-2">
                              {steps.map((step, idx) => {
                                const isActive = uploadProgress === idx;
                                const isCompleted = uploadProgress > idx;

                                return (
                                  <div key={idx} className="flex items-center gap-2 text-xs">
                                    <div className={`flex-shrink-0 w-4.5 h-4.5 rounded-full flex items-center justify-center 
                                      ${isCompleted ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 
                                        isActive ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' : 'bg-slate-900 text-slate-600 border border-slate-850'}`
                                    }>
                                      {isCompleted ? <BiCheck size={12} /> : isActive ? <span className="w-1.5 h-1.5 bg-indigo-400 rounded-full animate-pulse" /> : <div className="w-1 h-1 bg-slate-600 rounded-full" />}
                                    </div>
                                    <span className={`${isCompleted ? 'text-slate-400 font-medium' : isActive ? 'text-indigo-400 font-semibold' : 'text-slate-600'}`}>
                                      {step}
                                    </span>
                                  </div>
                                );
                              })}
                            </div>
                          </motion.div>
                        )}
                      </AnimatePresence>

                      {/* Upload Button */}
                      <button
                        onClick={handleUpload}
                        disabled={!file || isUploading}
                        className={`w-full h-[54px] rounded-xl font-bold text-white transition-all flex items-center justify-center gap-2 cursor-pointer
                          ${(!file || isUploading) 
                            ? 'bg-slate-900 border border-slate-850 text-slate-500 cursor-not-allowed shadow-none' 
                            : 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 shadow-[0_4px_20px_rgba(79,70,229,0.25)] hover:shadow-[0_4px_25px_rgba(79,70,229,0.4)] active:scale-[0.98]'
                          }
                        `}
                      >
                        {isUploading ? (
                          <>
                            <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            <span>Processing Resume...</span>
                          </>
                        ) : (
                          <>
                            <BiChip size={18} />
                            <span>Screen Resume Profile</span>
                          </>
                        )}
                      </button>
                    </div>

                    {/* Badges Footer */}
                    <div className="pt-6 border-t border-slate-850/80 flex flex-wrap justify-center gap-6">
                      {[
                        { icon: <BiCheckShield size={16} />, text: "Secure Storage" },
                        { icon: <BiLockAlt size={16} />, text: "Data Encryption" },
                        { icon: <BiFile size={16} />, text: "ATS Match System" }
                      ].map((badge, idx) => (
                        <div key={idx} className="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500">
                          <span className="text-slate-500">{badge.icon}</span>
                          {badge.text}
                        </div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </motion.div>
        </div>

      </div>
    </div>
  );
};

export default UploadResume;
