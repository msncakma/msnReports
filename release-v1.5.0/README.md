# 🐛 MSNReports v1.5.0 - Advanced Bug Reporting System

Hey there! 👋 Welcome to MSNReports, a powerful and user-friendly bug reporting plugin for Paper/Spigot servers. This plugin makes it super easy for your players to report bugs, and for staff to manage those reports effectively.

## ✨ Features

- 🎮 **Beautiful GUI Confirmation** - Interactive inventory-based confirmation system
- 🎯 **Multiple Discord Webhooks** - Separate channels for reports, status changes, admin notes
- 🗄️ **Database Choice** - Support for both SQLite and MySQL databases
- 🔒 **Secure Data Storage** - AES encryption for sensitive information
- 📊 **Advanced Report Management** - Full lifecycle tracking and status system
- 🛡️ **Permission-Based Access** - Granular control with detailed permissions
- 🎨 **Clean Discord Integration** - Rich embeds with automatic color code removal
- ⚡ **Thread Safety** - Proper async handling for smooth performance
- 🔄 **Auto Updates** - Built-in update checking and notifications
- 🛡️ **Rate Limiting** - Protection against spam and abuse
- 🌟 **Folia Compatible** - Full support for Folia server software

## 🎯 New in v1.5.0 (Stable Release)

- ✅ **Production Ready** - Stable release with all major bugs fixed
- ✅ **Fixed startup crashes** - Resolved NullPointerException during plugin initialization
- ✅ **Enhanced delete command** - Added debugging, force confirm option (`/mr delete <id> confirm`)
- ✅ **Improved reload system** - Better webhook configuration reloading with detailed feedback
- ✅ **Enhanced Discord messages** - Detailed reports with player stats, game mode, health, level
- ✅ **Better error handling** - Enhanced logging and troubleshooting capabilities
- ✅ **Cleaner console output** - Reduced verbose logging for better readability
- ✅ **Fixed GUI interactions** - Resolved confusing error messages during report submission

## 📋 Commands

### For Players
- `/report bug [description]` - Submit a new bug report with beautiful GUI
- `/reportbug [description]` - Alternative command (fully functional)
  - Permission: `msnreports.report`

### For Staff
- `/managereports` (alias: `/mr`) - Main command for managing reports
  - `/mr list [page]` - View all reports with pagination
  - `/mr view <id>` - View detailed report information
  - `/mr status <id> <status>` - Update report status
  - `/mr comment <id> <message>` - Add comments to reports
  - `/mr delete <id>` - Delete a report (with double confirmation)
  - `/mr delete <id> confirm` - Delete a report bypassing confirmation (for troubleshooting)
  - `/mr filter <status> [page]` - Filter reports by status
  - `/mr reload` - Reload plugin configuration with webhook status display
  - Permission: `msnreports.manage`

## 🚀 Installation

1. Download the latest release from [GitHub Releases](https://github.com/msncakma/msnReports/releases)
2. Place the .jar file in your server's `plugins` folder
3. Restart your server
4. Configure Discord webhooks in `config.yml`
5. Set up database preferences (SQLite by default)

## ⚙️ Configuration

### Basic Setup
```yaml
# License (required)
license: 'github.com/msncakma'

# Database configuration
database:
  type: 'sqlite' # or 'mysql'
  
# Discord webhook configuration
discord:
  enabled: true
  webhooks:
    reports:
      enabled: true
      url: 'YOUR_REPORTS_WEBHOOK_URL_HERE'
    admin-changes:
      enabled: true  
      url: 'YOUR_ADMIN_CHANGES_WEBHOOK_URL_HERE'
    admin-notes:
      enabled: true
      url: 'YOUR_ADMIN_NOTES_WEBHOOK_URL_HERE'
    status-changes:
      enabled: true
      url: 'YOUR_STATUS_CHANGES_WEBHOOK_URL_HERE'

# Update notifications
update-notifications:
  enabled: true
  check-on-startup: true
  notify-admins: true

# Encryption settings
encryption:
  key: 'your-32-character-encryption-key'
```

### MySQL Setup
```yaml
database:
  type: 'mysql'
  mysql:
    host: 'localhost'
    port: 3306
    database: 'msnreports'
    username: 'your_username'
    password: 'your_password'
```

### Discord Webhook Setup

1. **Create Discord Webhooks:**
   - Go to your Discord server settings
   - Navigate to Integrations → Webhooks
   - Create separate webhooks for different types of notifications
   - Copy the webhook URLs

2. **Configure Webhook Types:**
   - **Reports Webhook** - New bug reports from players
   - **Status Changes Webhook** - When report status is updated
   - **Admin Changes Webhook** - Administrative actions
   - **Admin Notes Webhook** - When comments are added

3. **Enable/Disable Individual Webhooks:**
   - Set `enabled: false` for any webhook type you don't want to use
   - The plugin will gracefully handle disabled webhooks

## 📊 Report Statuses

- **OPEN** - New reports awaiting review
- **IN_PROGRESS** - Reports being actively worked on  
- **RESOLVED** - Fixed reports
- **CLOSED** - Completed or dismissed reports
- **DUPLICATE** - Reports that duplicate existing issues
- **INVALID** - Reports that don't meet criteria

## 🔑 Permissions

### Basic Permissions
- `msnreports.report` - Allow players to report bugs (default: true)
- `msnreports.manage` - Access to report management (default: op)
- `msnreports.notify` - Receive admin notifications (default: op)

### Advanced Permissions  
- `msnreports.manage.view` - View bug reports
- `msnreports.manage.status` - Change report status
- `msnreports.manage.comment` - Add comments to reports
- `msnreports.manage.delete` - Delete reports
- `msnreports.admin.reload` - Reload plugin configuration
- `msnreports.admin.notify` - Receive update notifications

## 🔧 Troubleshooting

### Common Issues

**Plugin not starting:**
- Check your `license` is set to `github.com/msncakma` in config.yml
- Ensure you're using Paper/Spigot 1.21+ with Java 17+

**Discord notifications not working:**
- Verify webhook URLs are correct Discord webhook URLs
- Check webhook is enabled in config.yml
- Ensure Discord permissions allow the webhook to post

**Database errors:**
- For SQLite: Check file permissions in plugins/MSNReports/
- For MySQL: Verify connection details and database exists

**Delete command not working:**
- Use `/mr delete <id> confirm` to bypass confirmation for troubleshooting
- Check console for detailed error messages

### Debug Commands

```bash
# Check plugin status
/mr reload

# Force delete with confirmation bypass  
/mr delete <id> confirm

# View detailed report info
/mr view <id>
```

## 🔄 Migration from Older Versions

The plugin automatically migrates from older database schemas. No manual intervention required!

## 🤝 Contributing

Hey! Feel like making this plugin even better? Awesome! Here's how you can help:

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is under the MIT License - see the [LICENSE](LICENSE) file for details.

## 💖 Support

If you found this plugin helpful, don't forget to give it a star ⭐ on GitHub! Got questions? Feel free to:

- Open an [issue](https://github.com/msncakma/msnReports/issues)
- Contact me on Discord: msncakma
- Visit our community for support and updates

## 🙏 Credits

Made with ❤️ by [msncakma](https://github.com/msncakma)

Special thanks to the Paper/Spigot community for their excellent APIs and documentation.

---
*Stay awesome and keep your server bug-free! 🚀*