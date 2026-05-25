package com.talentcircle.adapter.in.seed;

import com.talentcircle.domain.model.*;
import com.talentcircle.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final WeeklyExecutionRepository executionRepository;
    private final CommunitySourceRepository sourceRepository;
    private final CommunityActivityRepository activityRepository;
    private final DraftRepository draftRepository;
    private final DraftVersionRepository versionRepository;
    private final PublicationRepository publicationRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      WeeklyExecutionRepository executionRepository,
                      CommunitySourceRepository sourceRepository,
                      CommunityActivityRepository activityRepository,
                      DraftRepository draftRepository,
                      DraftVersionRepository versionRepository,
                      PublicationRepository publicationRepository) {
        this.userRepository = userRepository;
        this.executionRepository = executionRepository;
        this.sourceRepository = sourceRepository;
        this.activityRepository = activityRepository;
        this.draftRepository = draftRepository;
        this.versionRepository = versionRepository;
        this.publicationRepository = publicationRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Users table not empty — skipping seed");
            return;
        }

        log.info("No users found — seeding example data");

        // 1. Admin user
        var admin = new User();
        admin.setEmail("admin@talentcircle.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setFullName("Admin TalentCircle");
        admin.setRole(User.Role.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Admin created: admin@talentcircle.com / admin123");

        // 2. Weekly execution (this week, completed)
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate friday = monday.plusDays(4);
        var execution = new WeeklyExecution();
        execution.setWeekStart(monday);
        execution.setWeekEnd(friday);
        execution.setStatus(WeeklyExecution.ExecutionStatus.COMPLETED);
        execution.setStartedAt(LocalDateTime.now().minusDays(1));
        execution.setCompletedAt(LocalDateTime.now().minusDays(1).plusHours(1));
        execution.setTriggeredBy("system");
        executionRepository.save(execution);

        // 3. Community source
        var source = new CommunitySource();
        source.setName("Discord TalentCircle");
        source.setType(CommunitySource.SourceType.DISCORD);
        source.setApiUrl("1018667130964688987");
        source.setActive(true);
        sourceRepository.save(source);

        // 4. Community activities
        var activities = List.of(
                createActivity(execution, source, CommunityActivity.ActivityType.POST,
                        "¡Java 21 ya está disponible con Virtual Threads!",
                        "Acabo de migrar nuestro microservicio a Java 21 y los Virtual Threads son increíbles. Redujimos la latencia en un 40% en operaciones I/O-bound. ¿Alguien más los está probando? Compartan sus experiencias!",
                        "CarlosM", 12, 5, 3, "1018667130964688987", "1502683916463636540"),
                createActivity(execution, source, CommunityActivity.ActivityType.QUESTION,
                        "¿Cómo configuro Spring Boot 3 con PostgreSQL en producción?",
                        "Estoy armando el deploy de mi primer proyecto con Spring Boot 3 y PostgreSQL. ¿Cuál es la mejor práctica para configurar el pool de conexiones? ¿Recomiendan HikariCP con valores específicos? Uso Docker + AWS.",
                        "AnaG", 8, 15, 2, "1018667130964688987", "1502683916463636541"),
                createActivity(execution, source, CommunityActivity.ActivityType.RESOURCE,
                        "Guía completa de Tailwind CSS v4",
                        "Acabo de publicar una guía actualizada de Tailwind CSS v4 con los nuevos features: container queries, improved custom properties, y el nuevo engine. Link al artículo completo en los comentarios.",
                        "LuisR", 4, 2, 10, "1018667130964688987", "1502683916463636542"),
                createActivity(execution, source, CommunityActivity.ActivityType.POST,
                        "Mi experiencia migrando a React 18 + TypeScript",
                        "Después de 3 meses migrando una app de 50k líneas de JS a TypeScript con React 18: 1) Los tipos redujeron bugs en producción un 60%. 2) El nuevo Suspense es game-changer. 3) TypeScript no es tan doloroso como parece al principio.",
                        "MariaF", 15, 7, 5, "1018667130964688987", "1502683916463636543"),
                createActivity(execution, source, CommunityActivity.ActivityType.QUESTION,
                        "¿Alguien ha probado Docker Desktop con WSL2?",
                        "Estoy considerando pasar de Docker Desktop a WSL2 directamente. ¿Alguien ya hizo el cambio? ¿Hay diferencias notables en performance o alguna contra? Uso Windows 11.",
                        "PedroM", 5, 9, 1, "1018667130964688987", "1502683916463636544"),
                createActivity(execution, source, CommunityActivity.ActivityType.RESOURCE,
                        "Los mejores plugins de IntelliJ IDEA para 2026",
                        "Recopilación de los plugins más útiles para IntelliJ IDEA en 2026: 1) Key Promoter X, 2) GitToolBox, 3) SonarLint, 4) String Manipulation, 5) .env files support. Dejen sus favoritos en los comentarios!",
                        "LauraS", 7, 4, 14, "1018667130964688987", "1502683916463636545"));

        activityRepository.saveAll(activities);

        // 5. Drafts
        var newsletterPending = new Draft();
        newsletterPending.setExecution(execution);
        newsletterPending.setChannel(Draft.Channel.NEWSLETTER);
        newsletterPending.setStatus(Draft.DraftStatus.PENDING);
        newsletterPending.setContent("""
                # Resumen Semanal TalentCircle #42
                
                ## 🚀 Java 21 y Virtual Threads
                
                Esta semana la comunidad vibró con la adopción de Java 21. CarlosM compartió su experiencia migrando microservicios a Virtual Threads, logrando una reducción del 40% en latencia. Varios miembros confirmaron mejoras similares en sus proyectos.
                
                ### Puntos clave:
                - Los Virtual Threads son ideales para operaciones I/O-bound
                - La migración desde Java 17 es relativamente sencilla
                - Se recomienda empezar por servicios con alta concurrencia
                
                ## ⚡ Spring Boot 3 y PostgreSQL
                
                AnaG abrió un debate muy productivo sobre configuración de Spring Boot 3 con PostgreSQL en producción. La comunidad coincidió en:
                
                1. **HikariCP** es el pool de conexiones recomendado
                2. Configurar `maximum-pool-size: 10` como punto de partida
                3. Usar `connection-timeout: 30000` para evitar timeouts en picos
                4. Siempre migrar con Flyway o Liquibase
                
                ## 🎨 Frontend: React 18 + TypeScript
                
                MaríaF documentó su migración de 50k líneas de JS a TypeScript, reportando una reducción del 60% en bugs de producción. El nuevo Suspense de React 18 recibió elogios por simplificar el manejo de estados de carga.
                
                ## 📚 Recursos destacados
                
                - **Tailwind CSS v4**: LuisR publicó una guía completa cubriendo container queries y el nuevo engine
                - **Plugins IntelliJ 2026**: LauraS recopiló los 5 plugins esenciales para el IDE
                - **Docker + WSL2**: Debate sobre performance y mejores prácticas
                
                ## 📅 Eventos de la semana
                
                No te pierdas nuestro próximo workshop: "Construyendo APIs resilient con Spring Boot 3" este viernes a las 17:00.
                
                ---
                *Generado automáticamente por TalentCircle Pipeline*
                *Comunidad de desarrolladores apasionados por la tecnología*""");
        newsletterPending.setAiScore(8.5);
        draftRepository.save(newsletterPending);

        var linkedinApproved = new Draft();
        linkedinApproved.setExecution(execution);
        linkedinApproved.setChannel(Draft.Channel.LINKEDIN);
        linkedinApproved.setStatus(Draft.DraftStatus.APPROVED);
        linkedinApproved.setContent("""
                🚀 **Java 21: El salto que tu equipo necesita**
                
                Esta semana en TalentCircle analizamos el impacto de Java 21 en equipos de desarrollo. Los Virtual Threads no son solo una promesa: equipos reportan hasta 40% menos latencia en operaciones I/O.
                
                💡 Tips para empezar:
                • Migra servicios con alta concurrencia primero
                • Spring Boot 3 + Java 21 es la combinación ideal
                • HikariCP con pool size = 10 para empezar
                
                📌 Si tu equipo aún está en Java 17, este es el momento de planificar la migración.
                
                #Java21 #Desarrollo #SpringBoot #VirtualThreads #TalentCircle""");
        linkedinApproved.setAiScore(7.2);
        linkedinApproved.setApprovedBy(admin);
        linkedinApproved.setApprovedAt(LocalDateTime.now().minusHours(2));
        draftRepository.save(linkedinApproved);

        var newsletterPublished = new Draft();
        newsletterPublished.setExecution(execution);
        newsletterPublished.setChannel(Draft.Channel.NEWSLETTER);
        newsletterPublished.setStatus(Draft.DraftStatus.PUBLISHED);
        newsletterPublished.setContent("""
                # Lo Mejor de la Comunidad: Abril 2026
                
                ## 🔥 Temas más discutidos
                
                ### 1. Java 21 y el boom de Virtual Threads
                
                La adopción de Java 21 ha sido el tema más comentado del mes. La comunidad está activamente migrando y compartiendo resultados impresionantes. La reducción de latencia en operaciones I/O-bound es el beneficio más reportado.
                
                ### 2. TypeScript en el Frontend
                
                Cada vez más equipos migran a TypeScript. La experiencia de MaríaF migrando 50k líneas sin incidentes mayores demuestra que las herramientas actuales hacen el proceso manejable.
                
                ### 3. DevOps para desarrolladores
                
                El debate sobre Docker Desktop vs WSL2 generó opiniones divididas, pero un consenso claro: la contenedorización local es indispensable en 2026.
                
                ## 📊 Estadísticas de la comunidad
                
                - 15 discusiones técnicas activas esta semana
                - 8 recursos compartidos
                - 23 miembros participaron activamente
                - 92% de rate de resolución en preguntas
                
                ## 🏆 Contribuidor destacado
                
                **CarlosM** por su análisis detallado de Virtual Threads y su disposición a ayudar a otros miembros en la migración.
                
                ## 📅 Próximos eventos
                
                - Workshop: APIs Resilientes con Spring Boot 3 (viernes 17:00)
                - Meetup: Arquitectura Hexagonal en la práctica (próximo martes)
                - Coding Session: Contribuciones Open Source (sábado 10:00)
                
                ---
                *TalentCircle - Comunidad de desarrolladores*
                *Únete a nuestra comunidad y sé parte de la conversación*""");
        newsletterPublished.setAiScore(9.1);
        newsletterPublished.setApprovedBy(admin);
        newsletterPublished.setApprovedAt(LocalDateTime.now().minusDays(1));
        draftRepository.save(newsletterPublished);

        // 6. Draft versions (v1 for each draft)
        var v1Pending = new DraftVersion();
        v1Pending.setDraft(newsletterPending);
        v1Pending.setContent(newsletterPending.getContent());
        v1Pending.setEditedBy("system");
        v1Pending.setVersionNumber(1);

        var v1Linkedin = new DraftVersion();
        v1Linkedin.setDraft(linkedinApproved);
        v1Linkedin.setContent(linkedinApproved.getContent());
        v1Linkedin.setEditedBy("system");
        v1Linkedin.setVersionNumber(1);

        var v1Published = new DraftVersion();
        v1Published.setDraft(newsletterPublished);
        v1Published.setContent(newsletterPublished.getContent());
        v1Published.setEditedBy("system");
        v1Published.setVersionNumber(1);

        versionRepository.saveAll(List.of(v1Pending, v1Linkedin, v1Published));

        // 7. Publication for the published draft
        var publication = new Publication();
        publication.setDraft(newsletterPublished);
        publication.setChannel(Publication.Channel.NEWSLETTER);
        publication.setStatus(Publication.PublicationStatus.SUCCESS);
        publication.setExternalPostId("pub_news_001");
        publication.setPublishedAt(LocalDateTime.now().minusHours(12));
        publication.setRetryCount(0);
        publicationRepository.save(publication);

        log.info("Seed complete: 1 source, 6 activities, 3 drafts, 3 versions, 1 publication");
    }

    private CommunityActivity createActivity(WeeklyExecution execution, CommunitySource source,
                                             CommunityActivity.ActivityType type, String title, String content,
                                             String author, int reactions, int responses, int shares,
                                             String guildId, String channelId) {
        var a = new CommunityActivity();
        a.setExecution(execution);
        a.setType(type);
        a.setSourceId(source.getId());
        a.setTitle(title);
        a.setContent(content);
        a.setAuthor(author);
        a.setReactionCount(reactions);
        a.setResponseCount(responses);
        a.setShareCount(shares);
        a.setPublishedAt(LocalDateTime.now().minusDays((long) (Math.random() * 7)));
        a.setDiscordMessageId("msg_" + System.nanoTime() + "_" + (int)(Math.random() * 100000));
        a.setDiscordChannelId(channelId);
        a.setSourceUrl("https://discord.com/channels/" + guildId + "/" + channelId + "/" + a.getDiscordMessageId());
        return a;
    }
}
