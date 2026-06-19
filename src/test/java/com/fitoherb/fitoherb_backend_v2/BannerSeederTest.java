package com.fitoherb.fitoherb_backend_v2;

import com.fitoherb.fitoherb_backend_v2.dtos.requests.BannerReq;
import com.fitoherb.fitoherb_backend_v2.services.BannerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootTest
public class BannerSeederTest {

    @Autowired
    private BannerService bannerService;

    @Test
    public void seedBanners() throws Exception {
        String frontendAssetsPath = "C:\\Users\\PC\\Documents\\GitHub\\fitoherb-frontend-v2\\public\\assets\\images\\banner";
        Path path = Paths.get(frontendAssetsPath);
        if (!Files.exists(path)) {
            System.out.println("Path does not exist: " + path);
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".jpg") || p.toString().toLowerCase().endsWith(".png"))
                    .collect(Collectors.toList());

            System.out.println("Found " + files.size() + " banner images to seed.");
            
            int position = 1;
            for (Path file : files) {
                File f = file.toFile();
                System.out.println("Processing file: " + f.getName());
                
                try (FileInputStream input = new FileInputStream(f)) {
                    MultipartFile multipartFile = new MockMultipartFile("image",
                            f.getName(), "image/jpeg", input);
                    
                    BannerReq req = new BannerReq();
                    // Example name: Banner-image-1.jpg
                    String title = f.getName().replace(".jpg", "").replace(".png", "").replace("-", " ");
                    req.setTitle(title);
                    req.setActive(true);
                    req.setPosition(position++);
                    
                    bannerService.createBanner(req, multipartFile);
                    System.out.println("Successfully seeded banner: " + title);
                }
            }
        }
    }
}
