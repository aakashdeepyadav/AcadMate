package com.acadmate.core.model

data class SubjectSyllabus(
    val subjectCode: String,
    val subjectName: String,
    val description: String,
    val credits: Int,
    val ltp: String, // e.g., "2:0:2"
    val units: List<SyllabusUnit>
)

data class SyllabusUnit(
    val title: String,
    val topics: List<String>
)

object PredefinedSyllabus {
    val bTechCse6thSem = listOf(
        SubjectSyllabus(
            subjectCode = "CSE225",
            subjectName = "DEVELOPING ANDROID APPS",
            description = "Building modern mobile applications using Kotlin and Jetpack Compose.",
            credits = 3,
            ltp = "2:0:2",
            units = listOf(
                SyllabusUnit("Unit 1: Fundamentals", listOf("Android Architecture", "Activities & Lifecycles", "Intents")),
                SyllabusUnit("Unit 2: UI with Compose", listOf("Layouts", "Modifiers", "State Management")),
                SyllabusUnit("Unit 3: Data & Networking", listOf("Room DB", "Retrofit", "MVVM Pattern")),
                SyllabusUnit("Unit 4: Advanced Features", listOf("WorkManager", "Notifications", "Firebase Integration"))
            )
        ),
        SubjectSyllabus(
            subjectCode = "CSE332",
            subjectName = "INDUSTRY ETHICS AND LEGAL ISSUES",
            description = "Understanding legal frameworks, IPR, and professional ethics in IT.",
            credits = 2,
            ltp = "2:0:0",
            units = listOf(
                SyllabusUnit("Unit 1: Legal Framework", listOf("Cyber Laws", "IT Act 2000", "Data Privacy")),
                SyllabusUnit("Unit 2: IPR", listOf("Patents", "Copyrights", "Trademarks")),
                SyllabusUnit("Unit 3: Professional Ethics", listOf("Code of Conduct", "Corporate Governance")),
                SyllabusUnit("Unit 4: Case Studies", listOf("Ethical Dilemmas in AI", "Social Media Regulation"))
            )
        ),
        SubjectSyllabus(
            subjectCode = "CSE357",
            subjectName = "COMBINATORIAL STUDIES",
            description = "Study of discrete structures, counting techniques, and graph theory.",
            credits = 3,
            ltp = "2:0:2",
            units = listOf(
                SyllabusUnit("Unit 1: Counting Principles", listOf("Permutations", "Combinations", "Binomial Theorem")),
                SyllabusUnit("Unit 2: Advanced Counting", listOf("Inclusion-Exclusion", "Generating Functions")),
                SyllabusUnit("Unit 3: Graph Theory", listOf("Planar Graphs", "Graph Coloring", "Hamiltonian Paths")),
                SyllabusUnit("Unit 4: Algorithms", listOf("Flow Networks", "Matching Theory"))
            )
        ),
        SubjectSyllabus(
            subjectCode = "INT345",
            subjectName = "COMPUTER VISION",
            description = "Image processing, feature detection, and deep learning for vision tasks.",
            credits = 3,
            ltp = "2:0:2",
            units = listOf(
                SyllabusUnit("Unit 1: Image Processing", listOf("Filtering", "Edge Detection", "Morphology")),
                SyllabusUnit("Unit 2: Feature Extraction", listOf("SIFT", "SURF", "HOG")),
                SyllabusUnit("Unit 3: Deep Learning for Vision", listOf("CNN Architecture", "Object Detection", "Segmentation")),
                SyllabusUnit("Unit 4: 3D Vision", listOf("Stereo Vision", "Structure from Motion"))
            )
        ),
        SubjectSyllabus(
            subjectCode = "PES319",
            subjectName = "SOFT SKILLS-II",
            description = "Advanced communication, leadership, and emotional intelligence.",
            credits = 3,
            ltp = "1:2:0",
            units = listOf(
                SyllabusUnit("Unit 1: Corporate Communication", listOf("Business Writing", "Presentation Skills")),
                SyllabusUnit("Unit 2: Leadership", listOf("Conflict Management", "Team Dynamics")),
                SyllabusUnit("Unit 3: EQ", listOf("Self-Awareness", "Empathy in Workplace")),
                SyllabusUnit("Unit 4: Career Prep", listOf("Mock Interviews", "Resume Building"))
            )
        )
    )
}
